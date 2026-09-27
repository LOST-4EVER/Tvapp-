# Release signing

## Why the key is committed to this repository

Android will only install an update over an existing app when the APK is signed
with **the same key** as the app already installed. If the key differs, the
install fails with:

```
App not installed as package conflicts with an existing package.
```

which is `INSTALL_FAILED_UPDATE_INCOMPATIBLE` underneath.

This project previously had **no** release key. `debug.keystore` is listed in
`.gitignore`, so the CI workflow ran `keytool -genkey` and generated a **brand
new random key on every single build**. Every published APK was therefore
signed differently, and no release could ever be installed over the previous
one — which is exactly the reported symptom.

## How it works now

* `debug.keystore.base64` holds the base64-encoded release key, committed so it
  is identical on every machine and every run.
* The workflow decodes it to `release-key.jks` and **fails the build** if the
  file is missing, rather than silently generating a replacement.
* `app/build.gradle.kts` picks the first signing key that exists, in order:
  1. `KEYSTORE_PATH` from the environment (a real Play upload key, if you have one)
  2. the committed `release-key.jks`
  3. the local `debug.keystore` — only a last resort, and it logs a warning

## Rules to keep updates working

1. **Never delete or regenerate `debug.keystore.base64`.** Doing so changes the
   signing fingerprint and every user must uninstall and reinstall.
2. **Never add a `keytool -genkey` fallback to the workflow.** A generated key is
   random per run, which is the original bug.
3. If you ever publish on Google Play with a real upload key, set
   `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` and `KEY_ALIAS` as
   repository secrets. Note that apps signed with the Play upload key cannot be
   replaced by these sideloaded builds, and vice versa — the two chains are
   separate.

## Verifying the signature

```bash
keytool -printcert -jarfile app-release.apk
```

Compare the SHA-256 fingerprint across two consecutive releases. They must match.

## Security note

The committed key is a throwaway used only for sideloaded GitHub releases, and
its password is the well-known `android`. That is acceptable while the app is
distributed as a GitHub APK. If you move to the Play Store, generate a private
upload key, keep it out of version control, and supply it through GitHub
secrets — and treat the already-published builds as a separate chain.
