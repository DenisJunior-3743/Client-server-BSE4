# Host your Android app on Reviden Store

This is the handoff guide for a developer publishing an Android app on
Reviden Store. It applies to native Android, Flutter, React Native, and
other frameworks that produce an Android APK. You do not need to modify
the Reviden server or install a Reviden SDK.

The downloadable **Hosting Guide** in Developer Docs includes the complete
GitHub Actions recipe and app integration reference as appendices, so you
can send a developer one Markdown file.

## 1. What goes in which project

| Reviden Store provides | The app developer provides |
|---|---|
| Projects, release storage, and dashboard | A signed, installable APK |
| Separate Production and Testing channels | The channel selected for each build |
| Public catalog and download pages | Project visibility and release notes |
| Version metadata and update policy API | Increasing version codes embedded in the APK |
| API tokens and CI upload/publish endpoints | Workflow and build configuration in the app's repository |
| Optional device approval and blocking | Update prompts, downloads, hash checks, and Android installation in the app |

Keep this guide with Reviden Store. Copy the CI recipe into the app's own
repository and adapt its build step. A broken request from a mobile app's
workflow must be fixed in that workflow; the store API contract stays the
same for every developer.

## 2. Collect these values

| Value | Where it comes from |
|---|---|
| Store base URL | The hosting team, e.g. `https://reviden-store.onrender.com`; use the HTTPS origin without `/api` or `/download` appended |
| Android package name | The final APK's `applicationId`, e.g. `com.example.myapp` |
| Project ID | On the Reviden Store website: **My Projects → your project → Copy project ID** (24 hexadecimal characters) |
| Release signing key | The existing keystore that signed installations you need to update |
| Channel | Exactly `testing` or `production` |
| Version name | A human-readable label, e.g. `1.2.0` or `1.2.0-beta.1` |
| Version code | A positive integer embedded in the APK; see §4 |
| API token | Required for automation; created by the project owner in Reviden's API Tokens page |

Store API tokens and signing passwords in CI secrets. The installed APK
needs the base URL, package/project identity, and channel, **not** your
management API token. Update-check and release-download endpoints do not
authenticate as the developer.

## 3. Create the project and prepare the APK

1. Sign up or sign in at the store URL. Open **My Projects → New project**.
2. Enter a name and the APK's exact package name. The package cannot be
   changed later and can belong to only one project on this store.
3. Choose **Public** or **Private**. Public projects appear in the catalog
   once a release is published. Private projects are hidden from the catalog
   and public latest-install/download links.
4. On the project's page, click **Copy project ID** in the **Project ID**
   box at the top. Keep this 24-character value for CI and permanent install
   links; paste it into your app repository's `REVIDEN_PROJECT_ID` GitHub
   Actions secret. Production and Testing use the same ID.
5. Build a release APK using your existing signing key. Keep a backup of
   that key and its credentials. Reviden neither signs nor rebuilds APKs.
6. Check that the final APK contains the expected package name, version
   name, and version code. Test installation on a device before shipping.

Submit an `.apk`, not an Android App Bundle (`.aab`). The multipart upload
limit is **300 MiB**. A universal APK is convenient for sharing one link
across devices; if you distribute an APK for a specific CPU architecture,
make sure it supports the devices receiving it.

Android requires compatible package identity and signing for an update.
Reviden checks the APK manifest against the project and computes a SHA-256
hash, but a successful upload is not proof that it can replace an APK
already installed on a phone. Test that upgrade path with the same key.

## 4. Channels, visibility, and version codes

Production and Testing have the same upload, publish, download, update
policy, grace-period, and device-control capabilities. They have separate
release histories and version-code sequences.

Choose the channel once, and use it consistently for:

1. CI version lookup and the final pre-upload recheck.
2. The APK's build configuration and tests.
3. The upload's `channel` field.
4. The installed app's `/api/updates/latest` requests.
5. Shared download/install links.

An app built for Testing must send `channel=testing`; omitting the channel
defaults to Production. A store with no published Testing release does not
fall back to Production. If your flavors use different package names, create
a separate project for each package. Flavor names themselves can differ
from channel names: a `beta` flavor can map to the store's `testing` channel.

**Visibility is project-wide.** Public lists published releases in both
catalog tabs. Private hides both. Installed apps can still check for updates
and use release-specific download URLs when a project is private. Visibility
is a discovery setting, not a login requirement on installed-app update APIs.

Every release must use a code not already recorded in its project and
channel. Publishing also requires a code higher than the currently published
release in that channel. For automation, use the authenticated release list:

```text
GET /api/projects/<project-id>/releases?channel=testing
Authorization: Bearer <API token>
```

The response is `{ "releases": [...] }`, sorted by descending code, up to
100 records. Include unpublished records when choosing the maximum: their
codes are already occupied. Compute:

```text
next code = max(highest recorded code in the selected channel, VERSION_CODE_FLOOR) + 1
```

An empty channel starts at `1`; code `1` advances to `2`. Production `20`
and Testing `23` yield `21` for Production or `24` for Testing. The optional
floor covers higher versions distributed manually or removed from history.
If a phone moves between channels with the same package, its destination
APK still needs a higher code than the installed APK; raise the floor when
needed. The CI recipe supports codes up to `2100000000`.

The lookup does not reserve a version code. Serialize CI releases and avoid
manual uploads during a run. Rerunning a failed upload needs inspection of
the dashboard first: the server may already have accepted it.

**Do not allocate CI versions with `/api/updates/latest?versionCode=0`.**
That endpoint expects an installed app's positive version code, so zero
returns HTTP 400. Passing `1` is also insufficient for allocation because
the API omits the release when the latest code is already `1`.

## 5. Publish your first release from the dashboard

1. Open the project and select **Testing**.
2. Choose **New release**, select the signed APK, and enter release notes.
   The store extracts the package name, version code, and version name.
3. Select **Optional** or **Forced**. When Forced is selected, enter
   **Grace period (days)**: `3` allows three days after publication; `0`
   requires the update immediately. The form defaults **Minimum version
   code** to `1` so all installed versions receive the chosen policy.
   Raise it only if older versions must update immediately, even during grace.
4. Wait for storage sync to finish. If it reports a sync failure, use
   **Retry storage sync** or follow the recovery message.
5. Click **Publish**. Confirm the status becomes `published`.
6. Download and install that Testing release, then test an upgrade from an
   older installed version. Repeat on Production when ready.

Uploading and publishing are distinct operations:

```text
upload accepted → syncing → uploaded → Publish → published
                  syncing → sync_failed (retry needed)
```

A `201` response from the upload API means the APK was accepted, not that
it is publicly downloadable. Publishing a newer release archives the previous
published release in that same channel. Testing is not automatically promoted
to Production: build/upload it to the intended channel explicitly.

## 6. Choose the update policy deliberately

| Field | Meaning |
|---|---|
| `releaseNotes` | Required, nonempty, up to 4000 characters; describe the change to users |
| `updateType` | `optional` or `forced` |
| `minimumVersionCode` | Installed versions below this code must update immediately |
| `forceUpdateGraceDays` | Whole days after publication before a forced update becomes mandatory; `0` means immediate |

The manual upload form and CI recipe both send a minimum code of `1` by
default. **Set `minimumVersionCode` explicitly in custom integrations.** If omitted on upload, the server
defaults it to the new release's code. That makes older installed versions
require an update even if `updateType=optional`. The minimum-code rule also
overrides a forced update's grace period.

Example optional release: code `12`, `updateType=optional`, minimum `1`.
Example forced release with time to prepare: code `12`, `updateType=forced`,
minimum `1`, grace `3`. A device below a chosen minimum still updates
immediately, regardless of grace.

For GitHub's **Run workflow** form, the provided recipe offers
`update_type`, `grace_days`, and `minimum_version_code`. For tag-triggered
builds, set repository variables `UPDATE_TYPE=forced`, `GRACE_DAYS=3`,
and `MINIMUM_VERSION_CODE=1` for a three-day grace period. Manual run inputs
take precedence over those variables. Both Production and Testing support
the same settings. Editing an existing release's grace days keeps its
original publication time as the start of the countdown.

## 7. Automate the same process in the app repository

Create an API token from **API Tokens** (`/app/tokens`) in the store and
save it when shown. Use a token belonging to the project owner; account
permissions must allow reading, creating, and publishing releases.

Configure these GitHub Actions secrets:

| Secret | Value |
|---|---|
| `REVIDEN_BASE_URL` | HTTPS store origin |
| `REVIDEN_PROJECT_ID` | The 24-character ID copied from the store website using **My Projects → your project → Copy project ID** |
| `REVIDEN_API_TOKEN` | The developer's management token |
| Signing secrets | Whatever your app's build uses for its existing release keystore |

A build-only dry run still needs the API token if it queries release history.
Keep your own JDK, Android SDK, Gradle, Flutter, or React Native setup in the
app repository. Reviden does not dictate those versions.

The complete CI recipe is included in the **GitHub Actions appendix** of
the downloaded guide. Its sequence is:

1. Resolve `production` or `testing` from the trigger/input.
2. Read the selected channel's release list and calculate the next code.
3. Build and sign an APK with that code and the matching update channel.
4. Run app tests and verify the APK's manifest and signing certificate.
5. Recheck the selected channel before upload.
6. Upload once, keep the returned release ID, and poll until `uploaded`.
7. Publish that release ID and require `status=published` before reporting success.

Relevant endpoints:

| Method and path | Authentication | Purpose |
|---|---|---|
| `GET /api/projects/<id>/releases?channel=testing` | Bearer token | CI version lookup |
| `POST /api/projects/<id>/releases/from-apk` | Bearer token | Multipart upload; creates release in `syncing` |
| `GET /api/releases/<release-id>` | Bearer token | Poll storage status |
| `PATCH /api/releases/<release-id>/publish` | Bearer token | Make the uploaded release available |
| `POST /api/releases/<release-id>/apk/retry-sync` | Bearer token | Retry a failed storage sync |
| `GET /api/updates/latest` | None | Installed-app update check |
| `GET /api/updates/download/<release-id>` | None | Download a published release |

## 8. Implement updates inside the app

Hosting an APK makes it downloadable. In-app update prompts require code
in your app. On launch, on a suitable schedule, or from a **Check for
updates** button, call:

```text
GET https://reviden-store.onrender.com/api/updates/latest
    ?packageName=com.example.myapp
    &versionCode=4
    &channel=testing
```

Send the version code actually installed on the device, at least `1`.
Use `projectId` instead of `packageName` if you prefer. Do not embed your
CI API token in this request or in the APK.

Handle the response as follows:

| Response | App behavior |
|---|---|
| `updateAvailable: false` | No newer published release in this channel; no update prompt |
| `updateAvailable: true`, `updateRequired: false` | Show the release notes and offer an update the user may postpone |
| `updateAvailable: true`, `updateRequired: true` | Require the update in your app's UI |
| `graceDaysRemaining` | Explain when a forced update becomes mandatory |
| `showGraceReminder` | With a device ID, only show the timed reminder when this is `true` |
| `deviceStatus: pending` or `blocked` | Explain approval/blocking status; no release is offered |
| Request fails | Report/retry a network error; do not treat an error body as release metadata |

Download the returned `release.downloadUrl`, check the full SHA-256 against
`release.sha256`, and reject a mismatch. Download into app-managed storage
and hand the verified file to Android's installer. The app needs internet
access and an appropriate install flow, including
`REQUEST_INSTALL_PACKAGES`, handling the user's unknown-source permission,
and a configured `FileProvider`/`PackageInstaller` as appropriate.
Do not use a raw `file://` URI or promise silent installation on normal
consumer devices. Keep user data intact by updating the existing package.
Android's [distribution guide](https://developer.android.com/studio/publish)
explains the install-source permission flow.

The **APK Integration appendix** provides the HTTP response shapes and a
Kotlin reference to adapt to your framework. Flutter/React Native apps
implement the same HTTP contract and use their Android platform integration
for installation.

## 9. Share the correct download link

For a public project, share one of:

```text
https://reviden-store.onrender.com/download?projectId=<id>&channel=production
https://reviden-store.onrender.com/download?projectId=<id>&channel=testing
```

Each page downloads the latest published APK in its selected channel.
The public catalog at `/browse` and app details also offer Production and
Testing tabs. **My Projects → Download links for** chooses which link to copy.

For a direct latest-APK link:

```text
https://reviden-store.onrender.com/api/projects/<id>/install?channel=testing
```

Private projects do not expose these public install links. Their installed
apps can still follow release-specific links obtained from update checks.

## 10. Optional device controls and ongoing maintenance

Device tracking is opt-in. Generate a stable random device ID once, keep
it across app launches, and pass it as `deviceId` on update checks and
download requests. Optional `POST /api/updates/device-register` attaches
device metadata; include the selected channel in its JSON body. The
returned download URL does not add your device ID for you: append it when
downloading if you are using device controls.

Approval/blocking is scoped to the project and device ID, not a separate
identity per release channel. It is not a replacement for your app's own
authentication. Dashboard access to these advanced controls depends on
account permissions/grants. See the Device Tracking guide for details.

Keep your own release APKs, signing-key backups, and obfuscation mapping
files. Archived versions may be purged by the host's retention setting
(default two days); Reviden is not your permanent release archive.

## 11. Verify before giving the app to users

- The APK installs with the intended package, signing certificate, version,
  and update channel.
- A published Testing release appears in Testing; Production uses its own
  release. A missing channel shows no update instead of switching channels.
- An older installation detects, downloads, verifies, and installs an update
  without uninstalling or losing app data.
- Optional, forced, minimum-version, and grace-period behavior match your
  intended policy.
- Public/Private visibility behaves as expected in both catalog tabs.
- CI reports success only after the intended release is `published`.
- If using device controls, approval/blocking also works during downloads.

## 12. Troubleshooting

| Symptom | Check |
|---|---|
| HTTP 400 during version lookup | Use the authenticated release-list endpoint for CI. The installed-app endpoint rejects `versionCode=0`. |
| HTTP 401 | Token missing, revoked, or invalid; create/use the correct token. |
| HTTP 403 | Project ownership or account permissions; with device requests, approval/blocking status. |
| HTTP 404 downloading | Correct project/channel, public visibility for install links, and an actual `published` release. |
| HTTP 409 on upload | Package mismatch or an already-used version code in this channel. |
| HTTP 409 on publish | Storage not finished, or code not above the currently published version. |
| HTTP 400 on a large/invalid upload | Valid APK, accepted fields, and the 300 MiB application limit; an upstream proxy may return 413 instead. |
| `sync_failed` | Retry storage sync for the existing release. If its temporary file was lost, follow the dashboard's re-upload instructions. |
| Uploaded but absent from downloads | Wait for `uploaded`, then publish; upload success alone does not publish. |
| Optional update becomes mandatory | Check `minimumVersionCode`; do not omit it unintentionally. |
| Phone refuses installation | Package, signing key, code/downgrade, supported Android version/architecture, and install-source permission. |
| Workflow still runs an old script | A rerun of a tagged build uses that tag's old commit. Commit the fix and create a new tag. |

When asking the hosting team for help, share the project ID, selected
channel, release ID (if available), workflow step, HTTP status, and error
message. Keep API tokens, signing credentials, and private keys out of logs.

### Reference guides

- [GitHub Actions setup and complete scripts](https://reviden-store.onrender.com/api/docs/github-actions/download)
- [APK integration reference](https://reviden-store.onrender.com/api/docs/integration/download)
- [Optional device tracking](https://reviden-store.onrender.com/api/docs/device-tracking/download)
- [Reducing APK size](https://reviden-store.onrender.com/api/docs/minification/download)


---

# GitHub Actions appendix: build, upload, and publish

This recipe goes in **your app repository**. It uses Reviden's existing API
for either channel and needs no server customization. The generic store
steps are complete below; you supply the build adapter for your framework
and existing release signing configuration.

An upload is not a publish. The final script waits for storage sync, checks
the returned APK metadata and hash, and explicitly publishes the release.

## 1. Configure the app repository

Create the Reviden project first, with the APK's exact package name. Create
an API token in `/app/tokens` using the project owner's account. It needs
release read/create/publish permissions.

Get the project ID from the **Reviden Store website**:

1. Sign in at your store URL and open **My Projects** (`/app/projects`).
2. Click your project's name to open its page.
3. In the **Project ID** box at the top, click **Copy project ID**.
4. In your app's GitHub repository, open **Settings → Secrets and variables
   → Actions → Repository secrets**. Create or edit `REVIDEN_PROJECT_ID`
   and paste the copied value.

The value is exactly **24 hexadecimal characters** (letters `a`–`f` and
digits `0`–`9`). Copy only the ID, without quotes or spaces. The project
name, Android package name, release ID, and full page URL are different
values. The same project ID works for both Production and Testing, even
before your first release and for private projects.

If Actions fails with `REVIDEN_PROJECT_ID must be the project's
24-character ID`, replace that repository secret with the value copied
from the website, then rerun the failed workflow. If the copy button is
blocked by your browser, select the visible Project ID and copy it manually.

Set these **GitHub Actions secrets**:

| Secret | Value |
|---|---|
| `REVIDEN_BASE_URL` | `https://reviden-store.onrender.com`, without an `/api` suffix |
| `REVIDEN_PROJECT_ID` | The 24-character ID copied from the website: **My Projects → your project → Copy project ID** |
| `REVIDEN_API_TOKEN` | Owner's API token, including for build-only dry runs |
| `ANDROID_KEYSTORE_BASE64` | Base64 of the existing release keystore, if the adapter below decodes it |
| `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | Signing values consumed by your build configuration |

Set these **repository variables**:

| Variable | Purpose/default |
|---|---|
| `APP_PACKAGE` | Required: package name of the final APK |
| `TAG_CHANNEL` | Plain version tags publish here; default `production` in this example |
| `JAVA_VERSION` | JDK required by your project; example defaults to `17`, change to match your Android Gradle plugin |
| `VERSION_CODE_FLOOR` | Default `0`; highest code distributed outside this channel's retained history |
| `MINIMUM_VERSION_CODE` | For tag builds: default `1`; versions below this must update immediately, even during grace |
| `UPDATE_TYPE` | For tag builds: default `optional`; set to `forced` when required |
| `GRACE_DAYS` | For tag builds: default `0`; whole days from publication before a forced release becomes mandatory |

Do not commit keystores, tokens, or signing passwords. Keep the key that
signed existing installations. Retain release APKs and obfuscation mapping
files outside the store's archived-release retention window.

## 2. Add the workflow

Save as `.github/workflows/reviden-release.yml`. Copy the two helper scripts
in §3 and §5, and implement the build adapter in §4, before triggering it.
Save shell scripts with LF line endings (`*.sh text eol=lf` in `.gitattributes`).

Plain `v1.2.0`/`V1.2.0` tags use `TAG_CHANNEL`; prerelease tags such as
`v1.2.0-beta.1` always use Testing. Manual runs explicitly choose the channel
and version name. The example imposes no application-specific branch name.

For a manual run, choose `update_type`, `grace_days`, and
`minimum_version_code` in **Actions → Reviden release → Run workflow**.
For example, `forced`, `3`, and `1` gives all older versions three days
from publication to update. Manual inputs override the tag-build variables,
including an explicit `0` for an immediate forced update.

For automated tags, set repository **Settings → Secrets and variables →
Actions → Variables** to `UPDATE_TYPE=forced`, `GRACE_DAYS=3`, and
`MINIMUM_VERSION_CODE=1` for the same policy. These settings apply to both
channels in this example. Return `UPDATE_TYPE` to `optional` when subsequent
tagged releases should be optional. The script sends `GRACE_DAYS` as the
API field `forceUpdateGraceDays`; upload time does not start the countdown.

```yaml
name: Reviden release
on:
  push:
    tags: ["v*", "V*"]
  workflow_dispatch:
    inputs:
      channel:
        description: Release channel
        type: choice
        options: [testing, production]
        default: testing
        required: true
      version:
        description: Version name, for example 1.2.0 or 1.2.0-beta.1
        type: string
        required: true
      dry_run:
        description: Build and verify without uploading
        type: boolean
        default: true
      update_type:
        description: Update policy
        type: choice
        options: [optional, forced]
        default: optional
        required: true
      grace_days:
        description: Whole days from publication before a forced update is mandatory (0 means immediate)
        type: number
        default: 0
        required: true
      minimum_version_code:
        description: Versions below this must update immediately, even during grace (1 includes all versions)
        type: number
        default: 1
        required: true

permissions:
  contents: read
concurrency:
  group: reviden-release
  cancel-in-progress: false

jobs:
  release:
    runs-on: ubuntu-latest
    timeout-minutes: 60
    env:
      REVIDEN_BASE_URL: ${{ secrets.REVIDEN_BASE_URL }}
      REVIDEN_PROJECT_ID: ${{ secrets.REVIDEN_PROJECT_ID }}
      REVIDEN_API_TOKEN: ${{ secrets.REVIDEN_API_TOKEN }}
      APP_PACKAGE: ${{ vars.APP_PACKAGE }}
      VERSION_CODE_FLOOR: ${{ vars.VERSION_CODE_FLOOR || '0' }}
    steps:
      - uses: actions/checkout@v4

      - name: Select version and channel
        env:
          EVENT_NAME: ${{ github.event_name }}
          REF_NAME: ${{ github.ref_name }}
          INPUT_VERSION: ${{ inputs.version }}
          INPUT_CHANNEL: ${{ inputs.channel }}
          TAG_CHANNEL: ${{ vars.TAG_CHANNEL || 'production' }}
          INPUT_UPDATE_TYPE: ${{ inputs.update_type }}
          INPUT_GRACE_DAYS: ${{ inputs.grace_days }}
          INPUT_MINIMUM_CODE: ${{ inputs.minimum_version_code }}
          TAG_UPDATE_TYPE: ${{ vars.UPDATE_TYPE || 'optional' }}
          TAG_GRACE_DAYS: ${{ vars.GRACE_DAYS || '0' }}
          TAG_MINIMUM_CODE: ${{ vars.MINIMUM_VERSION_CODE || '1' }}
        run: |
          set -euo pipefail
          for key in REVIDEN_BASE_URL REVIDEN_PROJECT_ID REVIDEN_API_TOKEN APP_PACKAGE; do
            [ -n "${!key}" ] || { echo "Missing $key" >&2; exit 1; }
          done
          if [ "$EVENT_NAME" = workflow_dispatch ]; then
            version="$INPUT_VERSION"
            channel="$INPUT_CHANNEL"
            update_type="$INPUT_UPDATE_TYPE"
            grace_days="$INPUT_GRACE_DAYS"
            minimum_code="$INPUT_MINIMUM_CODE"
          else
            version="${REF_NAME#[vV]}"
            channel="$TAG_CHANNEL"
            [[ "$version" != *-* ]] || channel=testing
            update_type="$TAG_UPDATE_TYPE"
            grace_days="$TAG_GRACE_DAYS"
            minimum_code="$TAG_MINIMUM_CODE"
          fi
          [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z]+([.-][0-9A-Za-z]+)*)?$ ]] || exit 1
          case "$channel" in production|testing) ;; *) exit 1 ;; esac
          case "$update_type" in optional|forced) ;; *) echo 'Invalid update type' >&2; exit 1 ;; esac
          [[ "$grace_days" =~ ^(0|[1-9][0-9]{0,5})$ ]] || { echo 'Invalid grace days' >&2; exit 1; }
          [[ "$minimum_code" =~ ^[1-9][0-9]{0,9}$ ]] && (( minimum_code <= 2100000000 )) || { echo 'Invalid minimum code' >&2; exit 1; }
          if [ "$update_type" = optional ]; then grace_days=0; fi
          echo "CHANNEL=$channel" >> "$GITHUB_ENV"
          echo "VERSION_NAME=$version" >> "$GITHUB_ENV"
          echo "EXPECT_VERSION_NAME=$version" >> "$GITHUB_ENV"
          echo "UPDATE_TYPE=$update_type" >> "$GITHUB_ENV"
          echo "GRACE_DAYS=$grace_days" >> "$GITHUB_ENV"
          echo "MINIMUM_VERSION_CODE=$minimum_code" >> "$GITHUB_ENV"

      - name: Choose the selected channel's next version code
        id: code
        run: |
          set -euo pipefail
          code_file="$RUNNER_TEMP/reviden-code"
          : > "$code_file"
          GITHUB_OUTPUT="$code_file" bash .github/scripts/next-version-code.sh
          next_code="$(sed -n 's/^version_code=//p' "$code_file")"
          (( MINIMUM_VERSION_CODE <= next_code )) || { echo 'Minimum code exceeds the new release code' >&2; exit 1; }
          cat "$code_file" >> "$GITHUB_OUTPUT"

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ vars.JAVA_VERSION || '17' }}

      # Add your project's pinned Android SDK / Flutter / Node setup here.
      - name: Build, sign, test, and verify the APK
        env:
          VERSION_CODE: ${{ steps.code.outputs.version_code }}
          ANDROID_KEYSTORE_BASE64: ${{ secrets.ANDROID_KEYSTORE_BASE64 }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: bash .github/scripts/build-apk.sh

      - name: Keep the APK with this run
        uses: actions/upload-artifact@v4
        with:
          name: apk-${{ env.CHANNEL }}-${{ env.VERSION_NAME }}
          path: ${{ env.APK_PATH }}
          if-no-files-found: error

      - name: Recheck the code before uploading
        if: ${{ github.event_name != 'workflow_dispatch' || !inputs.dry_run }}
        env:
          BUILT_CODE: ${{ steps.code.outputs.version_code }}
        run: |
          set -euo pipefail
          check_file="$RUNNER_TEMP/reviden-version-check"
          : > "$check_file"
          GITHUB_OUTPUT="$check_file" bash .github/scripts/next-version-code.sh
          next_code="$(sed -n 's/^version_code=//p' "$check_file")"
          [ "$next_code" = "$BUILT_CODE" ] || { echo 'Channel changed during build; rebuild.' >&2; exit 1; }

      - name: Upload, wait for storage, and publish
        if: ${{ github.event_name != 'workflow_dispatch' || !inputs.dry_run }}
        env:
          VERSION_CODE: ${{ steps.code.outputs.version_code }}
        run: bash .github/scripts/publish-apk.sh

      - name: Remove the temporary signing key
        if: ${{ always() }}
        run: rm -f "$RUNNER_TEMP/reviden-release.jks"
```

All steps inherit the same channel and update policy set in the first step. A dry run still
reads authenticated version history but does not upload or publish. Use the
same concurrency group for any other workflow publishing this app, and avoid
manual uploads during a build. Concurrency does not reserve a server version
code or guarantee that every pending tag will run; inspect queued/cancelled
runs before shipping several versions at once.

## 3. Add `.github/scripts/next-version-code.sh`

This reads **only the selected channel** and accounts for codes already
occupied by drafts and uploads. Empty history starts at `1`. The minimum
code is independent of the human-readable version name.

Do not query `/api/updates/latest?versionCode=0` here: it returns HTTP 400.
The installed-app API is not a reliable CI allocator even with code `1`,
because it omits an already-current release. Permanent HTTP errors fail
without being retried as though the server were merely asleep.


```bash
#!/usr/bin/env bash
# Allocate above every recorded code in the channel being built (including drafts/uploads).
# The lookup does not reserve a code: serialize releases and recheck before uploading.
# Requires curl, jq, and a token with release read access, including for dry runs.
set -euo pipefail

fail() {
  echo "::error title=Version code::$*"
  echo "ERROR: $*" >&2
  exit 1
}

BASE_URL="${REVIDEN_BASE_URL:?REVIDEN_BASE_URL is not set}"
BASE_URL="${BASE_URL%/}"
PROJECT_ID="${REVIDEN_PROJECT_ID:-}"
API_TOKEN="${REVIDEN_API_TOKEN:-}"
CHANNEL="${CHANNEL:?CHANNEL is not set}"
EXPECT_NAME="${EXPECT_VERSION_NAME:?EXPECT_VERSION_NAME is not set}"
OUT="${GITHUB_OUTPUT:-/dev/stdout}"
FLOOR="${VERSION_CODE_FLOOR:-0}"
[[ "$FLOOR" =~ ^(0|[1-9][0-9]{0,9})$ ]] && (( FLOOR < 2100000000 )) ||
  fail "VERSION_CODE_FLOOR must be an integer from 0 to 2099999999."
[[ "$PROJECT_ID" =~ ^[a-fA-F0-9]{24}$ ]] || fail "REVIDEN_PROJECT_ID must be the project's 24-character ID. Copy it from the Reviden Store website: My Projects > your project > Copy project ID, then update the GitHub Actions repository secret."
PROJECT_ID="${PROJECT_ID,,}"
[ -n "$API_TOKEN" ] || fail "REVIDEN_API_TOKEN is required to read release versions, including for a dry run."
case "$CHANNEL" in production | testing) ;; *) fail "Invalid release channel." ;; esac
[[ "$BASE_URL" == https://* ]] || fail "REVIDEN_BASE_URL must use HTTPS."

response="$(mktemp)"
trap 'rm -f "$response"' EXIT
# Retry transient failures (including a sleeping host), not invalid 400/401/403 requests.
status="$(curl --silent --show-error --output "$response" --write-out '%{http_code}' \
  --retry "${STORE_RETRY:-6}" --retry-delay "${STORE_RETRY_DELAY:-10}" --retry-connrefused \
  --connect-timeout 30 --max-time 120 \
  -H "Authorization: Bearer ${API_TOKEN}" \
  --get --data-urlencode "channel=${CHANNEL}" \
  "${BASE_URL}/api/projects/${PROJECT_ID}/releases")" ||
  fail "Couldn't read ${CHANNEL} releases from Reviden Store. Check REVIDEN_BASE_URL and connectivity."

if [ "$status" != 200 ]; then
  jq -r '.error // empty' "$response" >&2 2>/dev/null || true
  case "$status" in
    400) hint="The request was rejected. Check REVIDEN_PROJECT_ID and CHANNEL." ;;
    401) hint="REVIDEN_API_TOKEN was rejected; create a valid API token." ;;
    403) hint="The token needs release read access for this project." ;;
    404) hint="Check REVIDEN_BASE_URL and REVIDEN_PROJECT_ID." ;;
    *) hint="The store is unavailable or returned an unexpected response." ;;
  esac
  fail "Couldn't read ${CHANNEL} releases (HTTP ${status}). ${hint}"
fi

# Validate before treating an empty list as a first release. /releases returns
# the highest 100 codes, including unpublished versions, so the maximum is present.
versions="$(jq -er --arg channel "$CHANNEL" --arg project "$PROJECT_ID" '
  if type != "object" then error("Expected release list object")
  elif .error != null or (.releases | type) != "array" then error("Missing release list")
  else .releases | map(
    if type != "object" then error("Invalid release")
    elif .channel != $channel or .projectId != $project then error("Wrong project or channel")
    elif (.versionCode | type) != "number" then error("Invalid version code")
    elif .versionCode < 1 or .versionCode > 2100000000 or .versionCode != (.versionCode | floor)
      then error("Invalid version code")
    elif (.versionName | type) != "string" then error("Invalid version name")
    elif (.versionName | length) == 0 then error("Empty version name")
    elif (.status as $status | ["draft", "syncing", "uploaded", "sync_failed", "published", "archived"] | index($status)) == null
      then error("Invalid status")
    else . end
  ) | [(map(.versionCode) | max // 0),
       (map(select(.status == "published")) | max_by(.versionCode) | .versionName // "")] | @tsv
  end
' "$response" | tr -d '\r')" || fail "Invalid ${CHANNEL} release list; refusing to guess a version code."
IFS=$'\t' read -r latest_code latest_name <<<"$versions"
[[ "$latest_code" =~ ^[0-9]+$ ]] || fail "The store returned an invalid version code."

highest=$((latest_code > FLOOR ? latest_code : FLOOR))
(( highest < 2100000000 )) || fail "No Android version codes remain above the current release."
next=$((highest + 1))

if [ "$latest_name" = "$EXPECT_NAME" ]; then
  fail "Version ${EXPECT_NAME} is already the latest published release on ${CHANNEL}. Create a new tag instead of re-running it."
fi

echo "version_code=$next" >>"$OUT"
echo "Version code (${CHANNEL})"
echo "  highest recorded : ${latest_code}"
echo "  configured floor : ${FLOOR}"
echo "  this release     : ${next}"
```

## 4. Implement `.github/scripts/build-apk.sh` for your app

This is the app-specific part. There is no universal build/signing command
for every Android framework or Gradle configuration. The adapter must:

- Consume `VERSION_CODE`, `VERSION_NAME`, and `CHANNEL` from the workflow.
- Embed the code and name in the APK manifest, and the channel in whatever
  configuration your update client reads.
- Use your release signing key, run the correct flavor's tests, and verify
  the resulting APK's manifest, signing certificate, and release build type.
- Set `APK_PATH` in `$GITHUB_ENV` to exactly one tested, signed APK.

Example for a native app with existing `production` and `beta` flavors:

```bash
#!/usr/bin/env bash
set -euo pipefail
case "$CHANNEL" in
  production) flavor=production; flavor_cap=Production ;;
  testing) flavor=beta; flavor_cap=Beta ;;
  *) echo 'Unsupported channel' >&2; exit 1 ;;
esac

# Configure your signingConfig to read KEYSTORE_PATH and the password/alias env vars.
export KEYSTORE_PATH="$RUNNER_TEMP/reviden-release.jks"
printf '%s' "$ANDROID_KEYSTORE_BASE64" | base64 --decode > "$KEYSTORE_PATH"
chmod +x gradlew
./gradlew ":app:test${flavor_cap}DebugUnitTest" ":app:assemble${flavor_cap}Release" \
  -PciVersionCode="$VERSION_CODE" -PciVersionName="$VERSION_NAME"

apk="app/build/outputs/apk/${flavor}/release/app-${flavor}-release.apk"
test -s "$apk"
# Use your installed Android build-tools to inspect the output before upload.
apksigner verify --verbose --print-certs "$apk"
aapt dump badging "$apk"
echo "APK_PATH=$apk" >> "$GITHUB_ENV"
```

Adapt the module name, variants, output filename, and build-tool paths to
your project. `apksigner` and `aapt` above must be on `PATH` (normally under
`$ANDROID_HOME/build-tools/<your-version>/`). Compare the displayed package,
code, name, and certificate with the expected values; add automated assertions
in your adapter. `apksigner verify` verifies the signature, while the printed
certificate must still be checked against your existing release key.

The `ciVersionCode` and `ciVersionName` properties above are **custom
properties your build must consume**. Passing them alone does not change
the manifest. For example, merge this into the app module's Kotlin DSL:

```kotlin
android {
    defaultConfig {
        versionCode = providers.gradleProperty("ciVersionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("ciVersionName").orNull ?: "1.0.0"
    }
}
```

Keep existing local version defaults instead of the illustrative `1` and
`1.0.0`. Configure your production flavor's update client to use `production`
and beta's to use `testing`. If your flavor adds a version-name suffix,
either disable that suffix in CI or set `VERSION_NAME` to the final manifest
value. The publish script checks the name actually returned by the store.

An existing signing configuration can read the adapter's environment:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(requireNotNull(System.getenv("KEYSTORE_PATH")))
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
}
```

Merge with your existing configuration rather than declaring a second
`release` signing config. If you adopt this example literally, it requires
the signing environment for Gradle configuration; preserve your existing
local/debug setup as needed.

For Flutter, set up your pinned Flutter version and existing Android release
signing, then use `flutter build apk --release --build-number="$VERSION_CODE"
--build-name="$VERSION_NAME" --dart-define=UPDATE_CHANNEL="$CHANNEL"` in the
adapter and export the resulting APK path. Your Dart update client must
actually read `UPDATE_CHANNEL`. If using flavors, also select the matching
`--flavor` and output path. React Native and other stacks follow the same
adapter contract using their own Android build and configuration.

## 5. Add `.github/scripts/publish-apk.sh`

This script requires Bash, curl, jq, and sha256sum, available on the standard
Ubuntu runner. It validates the server's package/channel/version/hash before
publishing. It sends an explicit minimum supported code (default `1`) so
an optional release does not accidentally force every older version.


```bash
#!/usr/bin/env bash
set -euo pipefail
fail() { echo "ERROR: $*" >&2; exit 1; }

BASE_URL="${REVIDEN_BASE_URL:?Set REVIDEN_BASE_URL}"
BASE_URL="${BASE_URL%/}"
PROJECT_ID="${REVIDEN_PROJECT_ID:?Set REVIDEN_PROJECT_ID}"
TOKEN="${REVIDEN_API_TOKEN:?Set REVIDEN_API_TOKEN}"
CHANNEL="${CHANNEL:?Set CHANNEL}"
APK_PATH="${APK_PATH:?Set APK_PATH}"
VERSION_CODE="${VERSION_CODE:?Set VERSION_CODE}"
VERSION_NAME="${VERSION_NAME:?Set VERSION_NAME}"
APP_PACKAGE="${APP_PACKAGE:?Set APP_PACKAGE}"
UPDATE_TYPE="${UPDATE_TYPE:-optional}"
MINIMUM_VERSION_CODE="${MINIMUM_VERSION_CODE:-1}"
GRACE_DAYS="${GRACE_DAYS:-0}"
NOTES="${RELEASE_NOTES:-Release ${VERSION_NAME}}"

[[ "$BASE_URL" == https://* ]] || fail "Use the HTTPS store origin."
[[ "$PROJECT_ID" =~ ^[a-fA-F0-9]{24}$ ]] || fail "Invalid project ID."
PROJECT_ID="${PROJECT_ID,,}"
case "$CHANNEL" in production|testing) ;; *) fail "Invalid channel." ;; esac
case "$UPDATE_TYPE" in optional|forced) ;; *) fail "Invalid update type." ;; esac
[[ "$VERSION_CODE" =~ ^[1-9][0-9]{0,9}$ ]] && (( VERSION_CODE <= 2100000000 )) || fail "Invalid version code."
[[ "$MINIMUM_VERSION_CODE" =~ ^[1-9][0-9]{0,9}$ ]] && (( MINIMUM_VERSION_CODE <= VERSION_CODE )) || fail "Invalid minimum code."
[[ "$GRACE_DAYS" =~ ^(0|[1-9][0-9]{0,5})$ ]] || fail "Invalid grace days."
[ -s "$APK_PATH" ] || fail "APK missing or empty."

response="$(mktemp)"
trap 'rm -f "$response"' EXIT
local_sha="$(sha256sum "$APK_PATH" | cut -d ' ' -f 1)"

# Upload once. If the response is lost, inspect the dashboard before retrying.
curl --fail-with-body --silent --show-error --connect-timeout 30 --max-time 900 \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "apk=@${APK_PATH};type=application/vnd.android.package-archive" \
  --form-string "channel=${CHANNEL}" \
  --form-string "updateType=${UPDATE_TYPE}" \
  --form-string "minimumVersionCode=${MINIMUM_VERSION_CODE}" \
  --form-string "forceUpdateGraceDays=${GRACE_DAYS}" \
  --form-string "releaseNotes=${NOTES}" \
  "${BASE_URL}/api/projects/${PROJECT_ID}/releases/from-apk" > "$response" ||
  fail "Upload failed or its result is uncertain. Inspect this project's releases before retrying."

release_id="$(jq -er '.id | select(type == "string")' "$response" | tr -d '\r')"
[[ "$release_id" =~ ^[a-f0-9]{24}$ ]] || fail "Invalid release ID. Inspect the dashboard."
echo "Upload accepted: release ${release_id} (${CHANNEL}). Waiting for storage."

validate_release() {
  jq -e --arg id "$release_id" --arg project "$PROJECT_ID" --arg channel "$CHANNEL" \
    --arg package "$APP_PACKAGE" --arg name "$VERSION_NAME" --arg sha "$local_sha" \
    --argjson code "$VERSION_CODE" '
      .id == $id and .projectId == $project and .channel == $channel and
      .packageName == $package and .versionName == $name and .versionCode == $code and .sha256 == $sha
    ' "$response" > /dev/null || fail "Release metadata or hash mismatch; publication stopped. Inspect ${release_id}."
}

validate_release
ready=false
for (( attempt=0; attempt<=60; attempt++ )); do
  status="$(jq -er '.status' "$response" | tr -d '\r')"
  case "$status" in
    uploaded|published) ready=true; break ;;
    sync_failed) fail "Storage sync failed for ${release_id}. Retry its storage sync in the dashboard." ;;
    syncing) ;;
    *) fail "Unexpected status '${status}' for ${release_id}; publication stopped." ;;
  esac
  (( attempt < 60 )) || break
  sleep 5
  curl --fail-with-body --silent --show-error --retry 3 --retry-connrefused \
    --connect-timeout 30 --max-time 120 -H "Authorization: Bearer ${TOKEN}" \
    "${BASE_URL}/api/releases/${release_id}" > "$response" ||
    fail "Couldn't read status for ${release_id}. Inspect it before retrying."
  validate_release
done
[ "$ready" = true ] || fail "Still waiting for ${release_id}; inspect it before uploading again."

if [ "$status" != published ]; then
  curl --fail-with-body --silent --show-error --request PATCH \
    --connect-timeout 30 --max-time 180 -H "Authorization: Bearer ${TOKEN}" \
    "${BASE_URL}/api/releases/${release_id}/publish" > "$response" ||
    fail "Publish result uncertain for ${release_id}. Inspect its status before retrying."
fi
validate_release
jq -e '.status == "published"' "$response" > /dev/null || fail "Release is not published."
echo "Published ${CHANNEL} release ${release_id}: ${VERSION_NAME} (${VERSION_CODE})."
if [ -n "${GITHUB_OUTPUT:-}" ]; then echo "release_id=${release_id}" >> "$GITHUB_OUTPUT"; fi
```

## 6. Try it and recover from failures

1. Commit the workflow, both store scripts, and your build adapter to the
   app repository. The workflow must be on the default branch for GitHub's
   **Run workflow** button to appear.
2. Start a manual Testing run with **dry_run** enabled. Verify the saved APK
   on a device, including signing and the update channel.
3. Start a new Testing run with **dry_run** disabled. Confirm the final log
   says `Published testing release ...` and the dashboard says `published`.
4. Test an in-place update from an older APK. Then choose Production or push
   a plain version tag according to `TAG_CHANNEL`.

A transport failure after upload can still mean the release was accepted.
Keep the printed release ID and inspect it before retrying. If sync failed,
use the dashboard's retry action. If the APK is already `uploaded`, publish
that existing release. If it is `published`, the release is complete; do
not upload it again just because a later artifact step failed.

The store's exact recovery calls, with the same Bearer token, are:

```text
GET   /api/releases/<release-id>
POST  /api/releases/<release-id>/apk/retry-sync
PATCH /api/releases/<release-id>/publish
```

The allocator rejects a matching latest published version name on the target
channel. A code already taken by an unfinished upload is skipped. For a
failed tag whose workflow needs code changes, commit the fix and create a
new tag; rerunning the old tag checks out the old helper scripts.

Changing channels is an explicit new upload/publish operation. There is no
automatic promotion endpoint. If using the same package across channels,
account for Android's installed version code with `VERSION_CODE_FLOOR`.

Reference: [GitHub workflow syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)
and [Android release preparation](https://developer.android.com/studio/publish/preparing).


---

# Reviden Store — APK Integration Guide

For account/project setup, signing, channels, and the complete CI publishing
recipe, start with the [complete hosting guide](https://reviden-store.onrender.com/api/docs/hosting/download).

This is the contract your Android app follows to check for updates,
download a verified APK, and let the user install it over an existing
installation. It uses HTTPS and Android's installation flow.

Reviden requires no SDK or proprietary APK metadata. Your app implements
the HTTP calls and update UI below, including the Android permissions and
installation configuration needed for that flow.

---

## 1. Before you upload anything

1. Sign up for a Reviden Store account and create a **project** with your
   app's exact Android **package name** (its `applicationId` in
   `build.gradle`/`build.gradle.kts`). This can never be changed later —
   it's your app's real identity, so get it right the first time.
2. Every release you publish under that project must keep the **same
   package name and the same signing key** as the one before it. This
   isn't a Reviden rule — it's Android itself: the OS refuses to install an
   "update" whose package name or signing certificate doesn't match what's
   already installed. Keep your existing release key for this recipe;
   changing it requires a separately planned and tested migration.
3. Every release needs a **version code** that is a plain, always-increasing
   integer (`1`, `2`, `3`, …) — this is the same `versionCode` Android
   itself already requires in every APK's manifest. Reviden Store rejects
   duplicate codes within a project and channel, and refuses to publish a
   code that isn't higher than the currently published release there.

---

## 2. The update-check call

Your app calls this on its own schedule (e.g. on launch, or on a timer) —
Reviden Store never pushes anything to your app.

```
GET https://reviden-store.onrender.com/api/updates/latest
      ?packageName=com.yourcompany.yourapp
      &versionCode=<the version code currently installed>
      &channel=production
```

You can address your project either by `packageName` (shown above) or by
`projectId` (on the store website, open **My Projects → your project** and
click **Copy project ID** in the **Project ID** box) — either works
identically. `channel` is `production` or `testing`, matching whichever
channel you published the release under; omit it and it defaults to
`production`.

Both channels support the same publishing, APK downloads, update policies,
grace periods, and device controls. A testing build must send
`channel=testing` when checking for updates; the server only returns releases
from that channel and does not fall back to production.

### Public downloads for either channel

The public **Browse Apps** catalog and each app's detail and download pages
have **Production** and **Testing** tabs. A public project appears in a tab
once it has a published release in that channel. Drafts are never listed,
and version history stays within the selected channel.

Share a channel-specific download page:

```text
https://reviden-store.onrender.com/download?projectId=<project-id>&channel=production
https://reviden-store.onrender.com/download?projectId=<project-id>&channel=testing
```

The download button serves the latest published APK in that channel. The
permanent direct install endpoint also accepts `channel=testing` or
`channel=production`: `/api/projects/<project-id>/install?channel=testing`.
In **My Projects**, use **Download links for** to choose which channel's link
to copy or open.

The project's existing **Public / Private** setting applies to both
channels. Private projects are hidden from both catalog tabs and their
public latest-download/install links are unavailable. Installed apps can
still check for updates and use the release-specific download URL returned
by the update API in either channel.

### Optional device tracking

Optionally add `&deviceId=<a UUID you generate once and persist>` to opt
into per-device tracking/blocking — see
[the device tracking guide](https://reviden-store.onrender.com/api/docs/device-tracking/download) if you want
your project's dashboard to show which installations are checking in, and
to be able to block or require approval for specific ones. Omit it entirely
and everything above works exactly as documented, unchanged.

### Update-check responses

**Response, no update needed:**
```json
{ "updateAvailable": false, "updateRequired": false, "currentVersionCode": 4 }
```

**Response, an update exists:**
```json
{
  "updateAvailable": true,
  "updateRequired": false,
  "currentVersionCode": 4,
  "release": {
    "id": "6a9f4e58e4666603c7a4c51c",
    "versionCode": 5,
    "versionName": "1.2.0",
    "updateType": "optional",
    "releaseNotes": "Bug fixes and performance improvements.",
    "sha256": "3923052aaa6cb1f885d428d38362aa1df0bec565653d34d3899a57f3e7b6ac30",
    "fileSize": 18234112,
    "downloadUrl": "https://reviden-store.onrender.com/api/updates/download/6a9f4e58e4666603c7a4c51c"
  }
}
```

`updateRequired` is `true` when either:
- you marked the release a **forced** update when publishing it (and,
  if you also set a grace period, that window has elapsed — see below),
  or
- the installed version code is below the release's **minimum version
  code** (a floor you set per release — use this to force-upgrade anyone
  stuck on a version too old to safely keep using, without forcing
  everyone through every single intermediate release). This floor always
  applies immediately, regardless of any grace period.

Your app's job: if `updateRequired` is true, don't let the user dismiss the
update prompt. If it's `false`, show it as a normal "an update is
available" notice they can postpone.

If you sent `deviceId`, the response also carries a `"deviceStatus"` field
(`"active"`, `"pending"`, or `"blocked"`) — see the device tracking guide.
A `"blocked"` or `"pending"` device gets `updateAvailable: false` and no
`release` at all, even if a newer version exists.

### Grace period for forced updates

A forced release can optionally carry a **grace period** (set as a number
of days when publishing) — the device is free to postpone it until that
many days after the release went live, then it becomes mandatory
automatically with no further action needed on your end. Nothing about
how you read `updateRequired` changes — it's simply `false` during the
window and flips to `true` once it passes.

While a forced release is still inside its grace window, the response
carries two extra fields:

```json
{
  "updateAvailable": true,
  "updateRequired": false,
  "currentVersionCode": 4,
  "graceDaysRemaining": 3,
  "showGraceReminder": true,
  "release": { "...": "..." }
}
```

- `graceDaysRemaining` — how many days until the update becomes mandatory.
  Use it for your reminder's copy (e.g. "You have 3 day(s) left to
  update").
- `showGraceReminder` — only present when you sent a `deviceId`. The
  server throttles this to `true` **at most once per 24 hours per
  device**, so the platform — not your app — owns the "once a day"
  cadence: show your reminder dialog only when this is `true`, and stay
  quiet on every other check-in in between. Without a `deviceId`, this
  field is omitted and your app is responsible for its own cadence using
  `graceDaysRemaining` alone.

---

## 3. Downloading and verifying the APK

Download the file from `release.downloadUrl`. Before invoking the installer,
**verify the SHA-256 hash matches `release.sha256`** — this confirms the
bytes you received are exactly what was published, not something corrupted
or tampered with in transit. Reject the install if it doesn't match.

```kotlin
fun sha256Of(file: File): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
```

## 4. Installing it

Your app needs the `REQUEST_INSTALL_PACKAGES` permission and must use
Android's own `PackageInstaller` (or a `FileProvider`-backed
`ACTION_VIEW`/`ACTION_INSTALL_PACKAGE` intent for older API levels) to hand
the downloaded file to the system installer — Reviden Store has no part in
the actual install step, that's entirely between your app and Android.

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.INTERNET" />
```

For the `FileProvider` reference client below, add this provider **inside**
your manifest's `<application>` element (merge with an existing provider
if your app already has one):

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/update_file_paths" />
</provider>
```

Create `app/src/main/res/xml/update_file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <cache-path name="updates" path="updates/" />
</paths>
```

Use a destination such as `File(context.cacheDir, "updates/update.apk")`;
create its parent directory before downloading. On Android 8+, check
`context.packageManager.canRequestPackageInstalls()` before launching the
installer. If false, open `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` for
`Uri.parse("package:${context.packageName}")`, let the user grant permission,
then check again when the app resumes. Declaring the permission alone does
not grant it. The app still relies on Android's user-confirmed install flow.

---

## 5. A minimal, generic reference client

This is deliberately small and framework-free — adapt it to however your
app already does networking (Retrofit, OkHttp directly, Ktor, whatever you
use elsewhere).

This example uses AndroidX Core (`FileProvider`), Kotlin coroutines, and
OkHttp. Add those libraries using versions compatible with your app.

```kotlin
package com.yourcompany.yourapp.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class UpdateCheckResult(
    val updateAvailable: Boolean,
    val updateRequired: Boolean,
    val versionCode: Int?,
    val versionName: String?,
    val releaseNotes: String?,
    val downloadUrl: String?,
    val sha256: String?,
)

class AppUpdateClient(
    private val baseUrl: String,          // e.g. "https://reviden-store.onrender.com"
    private val packageName: String,      // your app's applicationId
    private val channel: String = "production",
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun checkForUpdate(currentVersionCode: Int): UpdateCheckResult = withContext(Dispatchers.IO) {
        val url = "${baseUrl.trimEnd('/')}/api/updates/latest".toHttpUrl().newBuilder()
            .addQueryParameter("packageName", packageName)
            .addQueryParameter("versionCode", currentVersionCode.toString())
            .addQueryParameter("channel", channel)
            .build()
        val body = client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            check(response.isSuccessful) { "Update check failed: HTTP ${response.code}" }
            JSONObject(requireNotNull(response.body).string())
        }

        val release = body.optJSONObject("release")
        UpdateCheckResult(
            updateAvailable = body.optBoolean("updateAvailable"),
            updateRequired = body.optBoolean("updateRequired"),
            versionCode = release?.optInt("versionCode"),
            versionName = release?.optString("versionName"),
            releaseNotes = release?.optString("releaseNotes"),
            downloadUrl = release?.optString("downloadUrl"),
            sha256 = release?.optString("sha256"),
        )
    }

    suspend fun downloadAndVerify(downloadUrl: String, expectedSha256: String, destination: File): Boolean =
        withContext(Dispatchers.IO) {
            destination.parentFile?.mkdirs()
            client.newCall(Request.Builder().url(downloadUrl).build()).execute().use { response ->
                check(response.isSuccessful) { "Download failed: HTTP ${response.code}" }
                val body = requireNotNull(response.body) { "Empty download response" }
                destination.outputStream().use { output -> body.byteStream().copyTo(output) }
            }

            val digest = MessageDigest.getInstance("SHA-256")
            destination.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            val matches = actual.equals(expectedSha256, ignoreCase = true)
            if (!matches) destination.delete()
            matches
        }

    fun launchInstall(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
```

Call `launchInstall` only after `downloadAndVerify` returns `true` and the
user has granted install-source permission. Handle thrown network/HTTP
errors in your app UI. This minimal client omits progress/cancellation and
device/grace-reminder fields; extend it for those optional features using
the response contract above. Any HTTP library can implement the same API.

---

## 6. Device tracking and blocking (optional)

Entirely opt-in — skip this section if you don't need it, and everything
in §2–§5 works exactly as documented with no `deviceId` at all.

If your project's dashboard has device tracking turned on, you can let it
show which installations are actually checking in, and block/unblock or
require-approve specific ones (an org distributing an internal app to its
own devices is the typical case — see the device tracking guide for the
full picture). Two things your app needs to do:

1. **Generate a device ID once, and persist it** — never regenerate it on
   every launch, or your project's device dashboard becomes meaningless
   (every launch would look like a brand-new install):
   ```kotlin
   object DeviceIdentity {
       private const val PREFS = "reviden_device"
       private const val KEY = "device_id"

       fun get(context: Context): String {
           val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           return prefs.getString(KEY, null) ?: java.util.UUID.randomUUID().toString().also {
               prefs.edit().putString(KEY, it).apply()
           }
       }
   }
   ```
2. **Send it** — add `&deviceId=...` to every `/api/updates/latest` call
   (§2's example client's `checkForUpdate` just needs one more
   `addQueryParameter("deviceId", deviceId)` call). Optionally, call
   `POST /api/updates/device-register` once at first launch (and again
   only if any of this metadata changes) to attach richer detail to the
   device's record in your dashboard:
   ```
   POST https://reviden-store.onrender.com/api/updates/device-register
   Content-Type: application/json

   {
     "projectId": "<your project id>",
     "deviceId": "<the persisted UUID from step 1>",
     "manufacturer": "Google",
     "model": "Pixel 8",
     "osVersion": "15",
     "label": "optional — any string your own app's login/user system wants
                Reviden Store to display alongside this device, e.g. an
                employee name or internal user ID; never a Reviden Store
                account"
   }
   ```
   Response: `{"status": "active" | "pending" | "blocked", "deviceId": "..."}`.
   A device you never explicitly register is still tracked/enforced (via
   the bare `deviceId` on `/latest`), just with less metadata attached.

A blocked or not-yet-approved device gets `deviceStatus` back from
`/latest` (see §2) instead of ever being told a newer version exists, and
gets a `403` from the download endpoints if it tries anyway — build your
UI around `deviceStatus` however makes sense for your app (e.g. a
"waiting for approval" screen for `"pending"`).

When downloading, explicitly append the same `deviceId` query parameter
to `release.downloadUrl`; the server's serialized URL does not add it for
you. Omitting `deviceId` bypasses the opt-in device lookup, so these controls
depend on your client sending its ID consistently.

---

## 7. What Reviden Store does **not** require

- No SDK, no special manifest tag, no build-time code generation.
- No API key for the update-check/download calls above — they're public by
  design, the same way an installed app checking the Play Store for
  updates doesn't authenticate as "you."
- No specific minimum SDK version, obfuscation setting, or build flavor.

The only real technical requirement is the one Android itself already
enforces: **keep the same package name and signing key across every
release.**

---

See also: the [GitHub Actions guide](/api/docs/github-actions/download)
for automating publishing a release every time you push a tag, the
[minification guide](/api/docs/minification/download) for keeping the APK
you publish as small as possible, and the
[device tracking guide](/api/docs/device-tracking/download) for the
optional per-device monitoring/blocking/approval module.

---

Developed by Denis Junior — 0790731277 · denisjunior3743@gmail.com
