#!/usr/bin/env bash
# Create an RSA4096 GPG signing key, upload it to keyserver.ubuntu.com, and
# save the public, private and revocation keys as .asc files next to this script.
set -euo pipefail

KEYSERVER="${KEYSERVER:-hkps://keyserver.ubuntu.com}"
OUT_DIR="$(cd "$(dirname "$0")" && pwd)"
GNUPGHOME="${GNUPGHOME:-$HOME/.gnupg}"

die() { echo "error: $*" >&2; exit 1; }

command -v gpg >/dev/null || die "gpg not found (install with: brew install gnupg)"

read -r -p "Full name: " NAME
[[ -n "${NAME// /}" ]] || die "name is required"
[[ "$NAME" != *[\<\>\(\)]* ]] || die "name must not contain < > ( )"

read -r -p "Email: " EMAIL
[[ "$EMAIL" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$ ]] || die "invalid email: $EMAIL"

read -r -s -p "Passphrase: " PASSPHRASE; echo
[[ -n "$PASSPHRASE" ]] || die "passphrase is required"
read -r -s -p "Confirm passphrase: " PASSPHRASE2; echo
[[ "$PASSPHRASE" == "$PASSPHRASE2" ]] || die "passphrases do not match"
unset PASSPHRASE2

read -r -p "Expiration (e.g. 1y, 6m, 90d, 0 = never) [2y]: " EXPIRE
EXPIRE="${EXPIRE:-2y}"

USER_ID="$NAME <$EMAIL>"
echo "Generating rsa4096 signing key for: $USER_ID (expires: $EXPIRE)"

STATUS="$(printf '%s' "$PASSPHRASE" | gpg --batch --pinentry-mode loopback --passphrase-fd 0 \
  --status-fd 1 --quick-gen-key "$USER_ID" rsa4096 sign "$EXPIRE")"
FPR="$(awk '/KEY_CREATED/ {print $4; exit}' <<<"$STATUS")"
[[ -n "$FPR" ]] || die "could not determine the new key fingerprint"
echo "Created key $FPR"

PREFIX="$OUT_DIR/${FPR: -16}"
PUBLIC_FILE="$PREFIX-public.asc"
PRIVATE_FILE="$PREFIX-private.asc"
REVOKE_FILE="$PREFIX-revocation.asc"

umask 077
gpg --armor --export "$FPR" > "$PUBLIC_FILE"
printf '%s' "$PASSPHRASE" | gpg --batch --pinentry-mode loopback --passphrase-fd 0 \
  --armor --export-secret-keys "$FPR" > "$PRIVATE_FILE"
unset PASSPHRASE

# gpg auto-generates a revocation certificate during key creation.
REV_SRC="$GNUPGHOME/openpgp-revocs.d/$FPR.rev"
[[ -f "$REV_SRC" ]] || die "revocation certificate not found at $REV_SRC"
cp "$REV_SRC" "$REVOKE_FILE"
chmod 644 "$PUBLIC_FILE"

echo "Uploading to $KEYSERVER ..."
if gpg --keyserver "$KEYSERVER" --send-keys "$FPR"; then
  UPLOADED="yes"
else
  UPLOADED="FAILED (retry: gpg --keyserver $KEYSERVER --send-keys $FPR)"
fi

cat <<EOF

Done.
  Fingerprint: $FPR
  Public key:  $PUBLIC_FILE
  Private key: $PRIVATE_FILE
  Revocation:  $REVOKE_FILE
  Uploaded:    $UPLOADED

Keep the private key and revocation certificate somewhere safe and out of git.
To use the revocation certificate, remove the leading ':' from its
"-----BEGIN PGP PUBLIC KEY BLOCK-----" line, then: gpg --import $(basename "$REVOKE_FILE")
EOF
