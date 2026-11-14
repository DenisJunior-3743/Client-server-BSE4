#!/usr/bin/env bash
# Chooses the version code for this release: one above every code recorded in
# EITHER channel (including drafts and unfinished uploads) and above
# VERSION_CODE_FLOOR.
#
# Reviden Store numbers each channel separately, but this is one app: a tester on
# beta code 7 must be able to move to the production release, and Android refuses to
# install a lower code over a higher one. Counting both channels keeps the codes
# rising whichever way a phone moves. The store still accepts it, because it only
# asks for a code above the one published in the channel being released to.
#
# The lookup does not reserve a code: releases are serialised by the workflow and
# the code is rechecked just before the upload.
# Requires curl, jq and a token with release read access, including for dry runs.
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

[[ "$FLOOR" =~ ^(0|[1-9][0-9]{0,9})$ ]] && ((FLOOR < 2100000000)) ||
  fail "VERSION_CODE_FLOOR must be an integer from 0 to 2099999999."
[[ "$PROJECT_ID" =~ ^[a-fA-F0-9]{24}$ ]] ||
  fail "REVIDEN_PROJECT_ID must be the project's 24-character ID. Copy it from the Reviden Store website: My Projects > your project > Copy project ID, then update the GitHub Actions repository secret."
PROJECT_ID="${PROJECT_ID,,}"
[ -n "$API_TOKEN" ] || fail "REVIDEN_API_TOKEN is required to read release versions, including for a dry run."
case "$CHANNEL" in production | testing) ;; *) fail "Invalid release channel." ;; esac
[[ "$BASE_URL" == https://* ]] || fail "REVIDEN_BASE_URL must use HTTPS."

# Reads one channel's releases into READ_CODE (the highest code recorded, 0 when the
# channel is empty) and READ_NAME (the name of its highest published release).
READ_CODE=0
READ_NAME=""
read_channel() {
  local channel="$1" response status hint versions
  response="$(mktemp)"

  # Retry transient failures (including a sleeping host), not invalid 400/401/403 requests.
  status="$(curl --silent --show-error --output "$response" --write-out '%{http_code}' \
    --retry "${STORE_RETRY:-6}" --retry-delay "${STORE_RETRY_DELAY:-10}" --retry-connrefused \
    --connect-timeout 30 --max-time 120 \
    -H "Authorization: Bearer ${API_TOKEN}" \
    --get --data-urlencode "channel=${channel}" \
    "${BASE_URL}/api/projects/${PROJECT_ID}/releases")" || {
    rm -f "$response"
    fail "Couldn't read ${channel} releases from Reviden Store. Check REVIDEN_BASE_URL and connectivity."
  }

  if [ "$status" != 200 ]; then
    jq -r '.error // empty' "$response" >&2 2>/dev/null || true
    rm -f "$response"
    case "$status" in
      400) hint="The request was rejected. Check REVIDEN_PROJECT_ID and the channel." ;;
      401) hint="REVIDEN_API_TOKEN was rejected; create a valid API token." ;;
      403) hint="The token needs release read access for this project." ;;
      404) hint="Check REVIDEN_BASE_URL and REVIDEN_PROJECT_ID." ;;
      *) hint="The store is unavailable or returned an unexpected response." ;;
    esac
    fail "Couldn't read ${channel} releases (HTTP ${status}). ${hint}"
  fi

  # Validate before treating an empty list as a first release. /releases returns the
  # highest 100 codes, including unpublished versions, so the maximum is present.
  versions="$(jq -er --arg channel "$channel" --arg project "$PROJECT_ID" '
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
  ' "$response" | tr -d '\r')" || {
    rm -f "$response"
    fail "Invalid ${channel} release list; refusing to guess a version code."
  }
  rm -f "$response"

  IFS=$'\t' read -r READ_CODE READ_NAME <<<"$versions"
  [[ "$READ_CODE" =~ ^[0-9]+$ ]] || fail "The store returned an invalid version code for ${channel}."
}

highest=$FLOOR
target_name=""
echo "Version code for the ${CHANNEL} channel"
for channel in production testing; do
  read_channel "$channel"
  echo "  highest recorded in ${channel}: ${READ_CODE}"
  if ((READ_CODE > highest)); then highest=$READ_CODE; fi
  if [ "$channel" = "$CHANNEL" ]; then target_name="$READ_NAME"; fi
done

((highest < 2100000000)) || fail "No Android version codes remain above the current release."
next=$((highest + 1))

if [ -n "$target_name" ] && [ "$target_name" = "$EXPECT_NAME" ]; then
  fail "Version ${EXPECT_NAME} is already the latest published release on ${CHANNEL}. Create a new tag instead of re-running it."
fi

echo "version_code=$next" >>"$OUT"
echo "  configured floor              : ${FLOOR}"
echo "  highest across both channels  : ${highest}"
echo "  this release                  : ${next}"
