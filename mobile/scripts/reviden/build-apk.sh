#!/usr/bin/env bash
# Kotlin/Gradle adapter for the Reviden release pipeline. Management tokens never enter Gradle.
set -euo pipefail
fail() { echo "::error title=Build::$*"; echo "ERROR: $*" >&2; exit 1; }
for name in CHANNEL VERSION_NAME VERSION_CODE APP_PACKAGE API_BASE_URL REVIDEN_BASE_URL \
  ANDROID_KEYSTORE_BASE64 KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD RUNNER_TEMP; do
  [ -n "${!name:-}" ] || fail "$name is not configured. See mobile/REVIDEN-RELEASE.md."
done
[ "$APP_PACKAGE" = com.trustbank.loanapp ] || fail "This release must keep the TrustBank package name."
[[ "$API_BASE_URL" == https://* ]] || fail "The release requires a public HTTPS backend."
case "$CHANNEL" in testing|production) ;; *) fail "Invalid channel." ;; esac
keystore="$RUNNER_TEMP/trustbank-release.jks"
umask 077
printf '%s' "$ANDROID_KEYSTORE_BASE64" | tr -d '[:space:]' | base64 --decode > "$keystore" || fail "Invalid keystore Base64."
[ -s "$keystore" ] || fail "The keystore is empty."
export KEYSTORE_PATH="$keystore"
export KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD
normalise() { tr -d ': \r\n' | tr 'A-F' 'a-f'; }
info="$(keytool -list -v -keystore "$keystore" -storepass:env KEYSTORE_PASSWORD -alias "$KEY_ALIAS" 2>&1)" || fail "The existing release key could not be opened."
key_sha="$(sed -n 's/^[[:space:]]*SHA256: *//p' <<< "$info" | head -n1 | normalise)"
pinned="$(normalise < mobile/release-signing-cert.sha256)"
[ -n "$key_sha" ] && [ "$key_sha" = "$pinned" ] || fail "The supplied key differs from the app's published signing certificate."
if [ -n "${RELEASE_CERT_SHA256:-}" ]; then
  [ "$(printf '%s' "$RELEASE_CERT_SHA256" | normalise)" = "$pinned" ] || fail "RELEASE_CERT_SHA256 differs from the pinned certificate."
fi
chmod +x mobile/gradlew
(cd mobile && ./gradlew :app:assembleRelease :app:testReleaseUnitTest --no-daemon --max-workers=2 \
  -PciVersionCode="$VERSION_CODE" -PciVersionName="$VERSION_NAME" \
  -PapiBaseUrl="$API_BASE_URL" -PrevidenBaseUrl="$REVIDEN_BASE_URL" -PrevidenChannel="$CHANNEL")
built=mobile/app/build/outputs/apk/release/app-release.apk
[ -s "$built" ] || fail "Release APK was not generated."
python3 mobile/scripts/reviden/verify-apk.py --apk "$built" \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" --channel "$CHANNEL" \
  --api-base-url "$API_BASE_URL" --store-base-url "$REVIDEN_BASE_URL" --github-env "$GITHUB_ENV"
