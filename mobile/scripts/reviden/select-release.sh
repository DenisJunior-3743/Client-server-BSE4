#!/usr/bin/env bash
# Works out what this run releases, from the tag that started it (or the inputs of a
# manual run): the version name, the Reviden channel and the update policy.
#
#   Tag                     Version         Channel     Update
#   V1.0.0                  1.0.0           production  optional
#   V1.0.0+forced           1.0.0           production  forced, with a grace period
#   V1.0.0.beta.0           1.0.0-beta.0    testing     optional
#   V1.0.0-beta.0           1.0.0-beta.0    testing     optional
#   V1.0.0.beta.0+forced    1.0.0-beta.0    testing     forced, with a grace period
#
# A pre-release (anything after the three numbers) always goes to testing. A plain
# version goes to $TAG_CHANNEL (production). The store's version names use a hyphen
# for the pre-release part, so 1.0.0.beta.0 is converted to 1.0.0-beta.0.
#
# Writes CHANNEL, VERSION_NAME, EXPECT_VERSION_NAME, UPDATE_TYPE, GRACE_DAYS and
# MINIMUM_VERSION_CODE to $GITHUB_ENV (when set) and prints them.
set -euo pipefail

fail() {
  echo "::error title=Release selection::$*"
  echo "ERROR: $*" >&2
  exit 1
}

EVENT_NAME="${EVENT_NAME:?EVENT_NAME is not set}"
REF_NAME="${REF_NAME:-}"
TAG_CHANNEL="${TAG_CHANNEL:-production}"
DEFAULT_GRACE_DAYS="${DEFAULT_GRACE_DAYS:-4}"
DEFAULT_UPDATE_TYPE="${DEFAULT_UPDATE_TYPE:-optional}"
DEFAULT_MINIMUM_CODE="${DEFAULT_MINIMUM_CODE:-1}"

if [ "$EVENT_NAME" = workflow_dispatch ]; then
  version="${INPUT_VERSION:?Enter a version name}"
  channel="${INPUT_CHANNEL:?Choose a channel}"
  update_type="${INPUT_UPDATE_TYPE:-optional}"
  grace_days="${INPUT_GRACE_DAYS:-$DEFAULT_GRACE_DAYS}"
  minimum_code="${INPUT_MINIMUM_CODE:-$DEFAULT_MINIMUM_CODE}"
  version="${version#[vV]}"
else
  tag="$REF_NAME"
  version="${tag#[vV]}"
  update_type="$DEFAULT_UPDATE_TYPE"
  case "$version" in
    *+forced)
      update_type=forced
      version="${version%+forced}"
      ;;
    *+*)
      fail "Unknown suffix in the tag '$tag'. The only suffix is +forced, for example V1.0.1+forced."
      ;;
  esac
  channel="$TAG_CHANNEL"
  grace_days="$DEFAULT_GRACE_DAYS"
  minimum_code="$DEFAULT_MINIMUM_CODE"
fi

# 1.0.0.beta.0 -> 1.0.0-beta.0 (the fourth part must start with a letter, so a
# four-number version such as 1.0.0.1 is still rejected below).
if [[ "$version" =~ ^([0-9]+\.[0-9]+\.[0-9]+)\.([A-Za-z][0-9A-Za-z]*(\.[0-9A-Za-z]+)*)$ ]]; then
  version="${BASH_REMATCH[1]}-${BASH_REMATCH[2]}"
fi

[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z]+([.-][0-9A-Za-z]+)*)?$ ]] ||
  fail "'$version' is not a version name. Use 1.2.0, or 1.2.0.beta.1 for a test build."

# A pre-release is a test build, whatever the tag channel is.
if [ "$EVENT_NAME" != workflow_dispatch ] && [[ "$version" == *-* ]]; then
  channel=testing
fi

case "$channel" in production | testing) ;; *) fail "Invalid channel '$channel'." ;; esac
case "$update_type" in optional | forced) ;; *) fail "Invalid update type '$update_type'." ;; esac
[[ "$grace_days" =~ ^(0|[1-9][0-9]{0,5})$ ]] || fail "Grace days must be a whole number, got '$grace_days'."
[[ "$minimum_code" =~ ^[1-9][0-9]{0,9}$ ]] && ((minimum_code <= 2100000000)) ||
  fail "The minimum version code must be a whole number from 1 to 2100000000."

# An optional update has no grace period: the person can always postpone it.
if [ "$update_type" = optional ]; then grace_days=0; fi

if [ -n "${GITHUB_ENV:-}" ]; then
  {
    echo "CHANNEL=$channel"
    echo "VERSION_NAME=$version"
    echo "EXPECT_VERSION_NAME=$version"
    echo "UPDATE_TYPE=$update_type"
    echo "GRACE_DAYS=$grace_days"
    echo "MINIMUM_VERSION_CODE=$minimum_code"
  } >>"$GITHUB_ENV"
fi

echo "Release selection"
echo "  channel      : $channel"
echo "  version name : $version"
if [ "$update_type" = forced ]; then
  if [ "$grace_days" = 0 ]; then
    echo "  update       : forced, mandatory immediately"
  else
    echo "  update       : forced, optional for $grace_days days after publication, then mandatory"
  fi
else
  echo "  update       : optional (people can postpone it)"
fi
echo "  minimum code : $minimum_code (older versions below this must update at once)"
