#!/usr/bin/env bash
# Revoke a GPG key using a revocation certificate and publish the revocation
# to keyserver.ubuntu.com.
# Usage: ./gpg-revoke.sh [revoke.asc]
set -euo pipefail

KEYSERVER="${KEYSERVER:-hkps://keyserver.ubuntu.com}"
REVOKE_FILE="${1:-revoke.asc}"

die() { echo "error: $*" >&2; exit 1; }

command -v gpg >/dev/null || die "gpg not found (install with: brew install gnupg)"
[[ -f "$REVOKE_FILE" ]] || die "file not found: $REVOKE_FILE"

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

# gpg prefixes auto-generated certificates with ':' to prevent accidental import.
sed 's/^:-----BEGIN PGP/-----BEGIN PGP/' "$REVOKE_FILE" > "$TMP"

PACKETS="$(gpg --list-packets "$TMP" 2>/dev/null)" || die "$REVOKE_FILE is not a valid OpenPGP file"
KEYID="$(awk '/^:signature packet:/ {for (i = 1; i <= NF; i++) if ($i == "keyid") {print $(i+1); exit}}' <<<"$PACKETS")"
[[ -n "$KEYID" ]] || die "no signature found in $REVOKE_FILE"
! grep -q 'key packet:' <<<"$PACKETS" && grep -q 'sigclass 0x20' <<<"$PACKETS" || die "$REVOKE_FILE is not a key revocation certificate"

if ! gpg --list-keys "$KEYID" >/dev/null 2>&1; then
  echo "Key $KEYID is not in your keyring, fetching it from $KEYSERVER ..."
  gpgconf --kill dirmngr 2>/dev/null || true
  gpg --keyserver "$KEYSERVER" --recv-keys "$KEYID" || die "could not fetch key $KEYID"
fi

gpg --list-keys --keyid-format long "$KEYID"

printf 'Are you sure you want to revoke key %s? (y/N) ' "$KEYID"
read -r ANSWER || ANSWER=""
[[ "$ANSWER" =~ ^[Yy]([Ee][Ss])?$ ]] || { echo "Aborted."; exit 1; }

gpg --import "$TMP"
gpg --list-keys "$KEYID" | grep -q revoked || die "key $KEYID does not show as revoked after import"
echo "Key $KEYID revoked locally."

echo "Uploading revocation to $KEYSERVER ..."
gpgconf --kill dirmngr 2>/dev/null || true
if gpg --keyserver "$KEYSERVER" --send-keys "$KEYID"; then
  echo "Revocation published."
else
  echo "Upload FAILED. Retry with: gpg --keyserver $KEYSERVER --send-keys $KEYID" >&2
  exit 1
fi
