#!/usr/bin/env bash
# Uploads the APK to Reviden Store, waits for storage, and publishes it. From the
# store's own recipe (Reviden store/github-actions.md); an upload is not a publish, and this
# script checks the returned package, channel, version and hash before it publishes.
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
