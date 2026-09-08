# Releasing jterm to Maven Central

Setup is done (pom release profile, LICENSE, this doc). Publishing is a
separate, deliberate act. **Do not publish without Joseph's explicit go-ahead.**

## One-time setup (not yet done)

1. **Central Portal account + namespace**
   - Create an account at https://central.sonatype.com (sign in with GitHub is fine).
   - Register/verify the namespace `io.jterm` — verification is a DNS TXT record
     on jterm.io: `_tcw-verification.jterm.io` → value provided by the portal.
     **Joseph must do this step** (DNS).
   - Generate a **user token** (Portal → Account → Generate User Token) —
     these are the credentials for `mvn deploy`, not the login password.

2. **GPG signing key** (Central rejects unsigned artifacts)
   ```bash
   gpg --full-generate-key            # RSA 4096, Phosphor BBS <noreply@phosphorbbs.net>
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEYID>
   ```
   Public key must go to a keyserver (ubuntu, `keys.openpgp.org`, or pgp.mit.edu).

3. **Credentials** — `~/.m2/settings.xml`:
   ```xml
   <settings>
     <servers>
       <server>
         <id>central</id>
         <username>PORTAL_USER_TOKEN_NAME</username>
         <password>PORTAL_USER_TOKEN_PASSWORD</password>
       </server>
     </servers>
   </settings>
   ```

## Release procedure (after setup)

```bash
# 0. jterm-bbs is NOT published — it's a private server app. Library only.

# 1. Version bump (no more -SNAPSHOT)
mvn versions:set -DnewVersion=0.1.0 && mvn versions:commit

# 2. Full build: tests + javadoc (must be warning-free) + sources + signing
mvn clean verify -Prelease

# 3. Stage & deploy (uploads to the portal, runs their validation)
mvn deploy -Prelease

# 4. Portal: review the deployment, click Publish (or set autoPublish=true first)

# 5. Tag the release
git tag -a v0.1.0 -m "jterm 0.1.0" && git push origin v0.1.0

# 6. Next development version
mvn versions:set -DnewVersion=0.1.1-SNAPSHOT && mvn versions:commit
```

## After the first publish

- Update jterm-website Quick Start: drop "build from source for now", keep the
  dependency snippet (version `0.1.0`, no `-SNAPSHOT`).
- Coordinate: jterm-bbs `pom.xml` pins jterm by version — bump its dependency
  after release if desired (but jterm-bbs itself stays unpublished).