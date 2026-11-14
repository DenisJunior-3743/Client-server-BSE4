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
