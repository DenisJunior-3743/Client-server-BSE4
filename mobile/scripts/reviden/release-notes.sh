#!/usr/bin/env bash
# Builds the release notes people read in the app's update prompt (the store allows
# up to 4000 characters) and exports them as RELEASE_NOTES.
#
#   1. If the run came from an annotated tag, its message is the notes:
#        git tag -a V1.0.0 -m "Faster sign-in. Fixes the map on small screens."
#   2. Otherwise the commit subjects since the previous tag.
#   3. Failing both, "Release <version>".
set -euo pipefail

tag="${RELEASE_TAG:-}"
version="${VERSION_NAME:-}"
notes=""

# Drops blank lines from the start and end.
trim_blank() {
  awk '{ a[n++] = $0 }
       END {
         s = 0; while (s < n && a[s] ~ /^[[:space:]]*$/) s++
         e = n - 1; while (e >= s && a[e] ~ /^[[:space:]]*$/) e--
         for (i = s; i <= e; i++) print a[i]
       }'
}

if [ -n "$tag" ] && [ "$(git cat-file -t "refs/tags/$tag" 2>/dev/null || true)" = tag ]; then
  notes="$(git tag -l --format='%(contents)' "$tag" | sed 's/[[:space:]]*$//' | trim_blank)"
fi

if [ -z "$notes" ]; then
  previous=""
  if [ -n "$tag" ]; then
    previous="$(git describe --tags --abbrev=0 "refs/tags/${tag}^" 2>/dev/null || true)"
  else
    previous="$(git describe --tags --abbrev=0 HEAD 2>/dev/null || true)"
  fi
  if [ -n "$previous" ]; then
    range="${previous}..HEAD"
  else
    range="HEAD"
  fi
  notes="$(git log --no-merges --pretty='- %s' -n 25 "$range" 2>/dev/null | trim_blank || true)"
fi

[ -n "$notes" ] || notes="Release ${version:-}"

# Stay inside the store's 4000-character limit, without cutting a character in half.
notes="$(printf '%s' "$notes" | head -c 3900 | iconv -c -f UTF-8 -t UTF-8 2>/dev/null || printf '%s' "$notes" | head -c 3900)"

echo "Release notes"
printf '%s\n' "$notes" | sed 's/^/  | /'

if [ -n "${GITHUB_ENV:-}" ]; then
  delimiter="NOTES_$(date +%s)_${RANDOM}"
  {
    echo "RELEASE_NOTES<<${delimiter}"
    printf '%s\n' "$notes"
    echo "${delimiter}"
  } >>"$GITHUB_ENV"
fi
