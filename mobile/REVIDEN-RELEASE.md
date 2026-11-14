# Build and publish TrustBank Android releases

This workflow adapts the existing FarmerStack release scripts for this Kotlin/Gradle app. The reference contracts supplied with this repository are in `Reviden store/hosting.md`, `integration.md` and `github-actions.md` on `main`.

## App identity and the current release

- Package: `com.trustbank.loanapp` (keep it for every release).
- Reviden project ID: `6aa2456a0dba506c0b97fdc3`.
- Store: `https://reviden-store.onrender.com`.
- Backend: `https://client-server-bse4-backend.vercel.app/`.
- Published version observed during setup: production `1.1.1`, code `2`.
- Local test APK: `1.2.0-beta.1`, code `3`, testing channel.
- The existing private release key was checked against the published APK and matches. Its public SHA-256 is pinned in `release-signing-cert.sha256`. Keep that key for updates.

## GitHub setup

Open the repository's **Settings > Secrets and variables > Actions**. Add these seven **repository secrets**:

| Secret | Value/source |
|---|---|
| `REVIDEN_BASE_URL` | `https://reviden-store.onrender.com` |
| `REVIDEN_PROJECT_ID` | `6aa2456a0dba506c0b97fdc3` (confirm in My Projects > TrustBank > Copy project ID) |
| `REVIDEN_API_TOKEN` | A token created by this project's owner at `/app/tokens`, with release read/create/publish permissions |
| `ANDROID_KEYSTORE_BASE64` | Base64 of the existing `mobile/keystore/release.jks` |
| `KEYSTORE_PASSWORD` | `storePassword` from ignored `mobile/keystore/keystore.properties` |
| `KEY_ALIAS` | `keyAlias` from that same private file |
| `KEY_PASSWORD` | `keyPassword` from that same private file |

Copy the keystore Base64 to your clipboard from PowerShell at the repository root, then paste it into that GitHub secret:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path '.\mobile\keystore\release.jks').Path)) | Set-Clipboard
```

The management token and signing values are used only on the CI runner. They are not embedded in the app. No Neon or Atlas credentials belong in this workflow or the APK. Repository secret values cannot be retrieved from GitHub later, so preserve the original private keystore and its passwords.

These optional **repository variables** control releases; the defaults are already configured:

| Variable | Default/purpose |
|---|---|
| `API_BASE_URL` | `https://client-server-bse4-backend.vercel.app/` |
| `TAG_CHANNEL` | `production` for plain version tags; prerelease tags always select testing |
| `VERSION_CODE_FLOOR` | `3`, accounting for the local APK distributed during setup |
| `UPDATE_TYPE` | `optional` |
| `GRACE_DAYS` | `4` for forced releases; optional updates always use zero |
| `MINIMUM_VERSION_CODE` | `1`, so an ordinary optional release does not force every older installation |
| `RELEASE_CERT_SHA256` | Optional extra check; must equal `release-signing-cert.sha256` if supplied |

A dry run also needs the Reviden token/project secrets because it reads authenticated version history. GitHub had no release secrets configured when this setup was inspected.

## Publish by pushing one tag

Commit and push the code to `main` or `mobile` first. Tags must point to a commit already on one of those branches. Push one release tag at a time and wait for the result.

Testing release:

```powershell
git switch main
git pull --ff-only origin main
git tag -a v1.2.0-beta.1 -m "Testing the borrower loan workflow"
git push origin v1.2.0-beta.1
```

Production release after testing:

```powershell
git tag -a v1.2.0 -m "Borrower applications, GridFS documents and staff processing"
git push origin v1.2.0
```

`V1.2.0.beta.1` is also accepted and becomes version `1.2.0-beta.1` on testing. `v1.2.1+forced` selects a forced production update using the configured grace period. A plain tag uses `UPDATE_TYPE`, which defaults to optional. Manual **Actions > Reviden release > Run workflow** supports an explicit channel/version/policy and a build-only dry run.

The workflow chooses a code above every recorded code in **both** channels (including drafts/uploads) and the configured floor. This lets a tester later install a higher-code production build. Lookup does not reserve a code: the workflow serializes runs and rechecks immediately before upload. Do not manually upload another release during a run. GitHub concurrency may replace an older pending run when several tags are pushed together.

The workflow builds with Java 21 and Android SDK/build-tools 34, signs with the existing key, and shrinks the APK using R8. It validates the package, versions, release flag, certificate, compiled backend/channel, checksum and mapping file. The run retains the APK, JSON build metadata, SHA-256 and R8 mapping as artifacts for 90 days.

Before uploading, the workflow requires `/actuator/health` to report HTTP 200 and `UP`. An unhealthy backend stops publication after preserving the signed APK artifact. It then uploads **once**, waits for storage to finish, checks the returned identity/version/hash, and explicitly publishes. Success requires the server to report `published`.

If upload/publish loses its response, inspect the project's releases before rerunning; the server may already have accepted it. A storage failure needs the dashboard's retry-sync action. Never move an already published tag to different code.

## Download links after publication

- Testing: `https://reviden-store.onrender.com/download?projectId=6aa2456a0dba506c0b97fdc3&channel=testing`
- Production: `https://reviden-store.onrender.com/download?projectId=6aa2456a0dba506c0b97fdc3&channel=production`

Public install links require a public Reviden project. Testing and production are separate histories: a testing APK checks testing updates, while a production APK checks production. To switch channels, install the appropriate higher-code APK from that channel's download link. The app verifies the download SHA-256 before handing it to Android's installer.

## Build locally

From the repository root:

```powershell
.\mobile\scripts\build-release.ps1 -VersionName 1.2.0-beta.1 -VersionCode 3 -Channel testing
```

The helper reuses `mobile/keystore/keystore.properties` and verifies the output using the same Python checker as CI. `JAVA_HOME` must point to Java 21; the helper can use Android Studio's bundled JDK when it is unset. Android SDK/build-tools 34 and Python are required. Artifacts are collected in ignored `mobile/releases/`. For future manual builds, use a code higher than every previously installed/distributed build.

An Android Studio debug build has a different signing key and must be uninstalled before switching to the signed release. An existing Reviden `1.1.1` installation uses the matching release key and can update in place. Android may ask you to allow installation from the chosen app/browser.

## Test the client-initiated loan flow

1. Install the signed testing APK. Its account settings show the Testing update channel.
2. Register/sign in as a **borrower** on mobile. Use the web dashboard's admin account to register staff.
3. Complete the borrower profile, select a loan product, fill the application and select the three document files.
4. Tap Submit. The mobile client sends the loan creation request with its idempotency key, then uploads the files to GridFS through the backend. Retry must resume that loan instead of creating a second one.
5. Sign into the web dashboard as the relevant staff: inspect/verify documents, initiate the credit check, and have the manager decide the application.
6. Return to mobile, accept an approved offer, and continue through disbursement/repayment testing.

The workflow never creates customer loans or transfers money. SMS, credit bureau and payment settlement remain the previously documented demo integrations. For controlled new-account testing without an SMS provider, the backend can explicitly enable `APP_OTP_DEV_ECHO=true` so the existing verification screen can show the test code; keep that off for real customer use.

The supplied hosted backend returned HTTP 500 from its health endpoint during setup. A valid APK cannot complete the loan flow until that deployment is healthy. Check the Vercel runtime startup error and the Neon/Atlas/environment configuration; do not assume an APK rebuild will fix a server startup failure.
