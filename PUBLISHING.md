## Instructions

### GPG Signing Key Creation

1. to create a new gpg key and send it to keyserver.ubuntu.com, execute
   ```shell
   ./scripts/gpg-keygen.sh
   ```
   and type your `Full Name` and `Email`
2. **BACKUP** the generated private/public/revocation keys in `asc` format
3. verify the key was uploaded
   ```shell
   gpg --keyserver hkps://keyserver.ubuntu.com --recv-keys <KEYID>
   ```
4. to revoke a key:
    - save the revoke key in a file called `revoke.asc`
    - run `./gpg-revoke.sh`
    - last check: make sure the `KEYID` printed is the one you want to revoke
    - hit `y`, then `Enter`

### Local Publishing

1. create a file `~/.sbt/sonatype_credentials` with the following content
   ```shell
   host=central.sonatype.com
   user=<sonatype user token>
   password=<password of sonatype user token>
   ```
2. create a file ` ~/.sbt/1.0/sonatype.sbt` with following content
   ```shell
   credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")
   ```
3. run
   ```shell
   crossReleaseAll
   ```
   and paste the GPG signing key password in the terminal

### GitHub Publishing

Make sure you have a [User Token in Sonatype](https://central.sonatype.com/usertoken), then provide the
following [secrets](https://github.com/polentino/redacted/settings/secrets/actions):

- `PGP_ARMORED_PRIVATE_SIGNING_KEY` : the signing private key in armored format
- `PGP_PRIVATE_SIGNING_KEY_PASSPHRASE` : the password of the signing private key
- `SONATYPE_USER_TOKEN` : the user token provided by Sonatype
- `SONATYPE_USER_TOKEN_PASSWORD` : the password associated to Sonatype's user token