# TrustBank Android borrower app

Open `mobile/` in Android Studio. All repositories use the Spring Boot API. Fake repositories and bundled sample users, applications, notifications and products have been removed. Only the backend can authenticate a borrower or save account changes.

## Backend URL

Builds default to the public backend `https://client-server-bse4-backend.vercel.app/` from `gradle.properties`. For a local Android emulator, override with `-PapiBaseUrl=http://10.0.2.2:8080/`. For a physical phone or your hosted backend, build with:

```powershell
.\gradlew.bat installDebug -PapiBaseUrl=https://<backend-domain>/
```

You can also supply `API_BASE_URL` in the build environment. The Gradle `apiBaseUrl` property takes precedence. This URL is public configuration; Neon/Atlas credentials stay on the backend. The app never falls back to sample data if the service is unavailable. Use a backend borrower account, complete OTP verification, and sign in.

JWTs are attached to HTTP requests. Loan submissions, offer acceptance and repayments reuse idempotency keys for retries. Document selection uses Android's system picker; PDF/JPEG/PNG uploads go through the API to GridFS. See [the GridFS setup guide](../backend/GRIDFS-SETUP.md).

## GitHub Actions

Yes, Android CI is configured. GitHub reads workflows from the **repository root**, so the workflow belongs in `.github/workflows/verify.yml`, outside `mobile/` in the main monorepo. The focused `mobile` branch also has a root `.github/workflows/verify.yml`.

The Android job installs Java 21 and Android SDK 34, then runs from `mobile/`:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon
```

It runs on pushes, pull requests and manual `workflow_dispatch`. Android unit tests cover bounded document-file reads. The instrumented keyboard check requires a connected test device or emulator; normal CI compiles the app and runs the unit tests. Existing release signing files remain private.

The Gradle wrapper uses 9.7.1 with Android plugin 8.4.2 and Kotlin 1.9.24. Package name, app version, signing configuration and Reviden update settings are preserved. See `VALIDATION.md` for validation and `integration.md` for APK update steps.

Current limits: JWTs and pending OTP verification live in memory. SMS delivery, credit-bureau and payment-provider integrations remain backend stubs; these are separate from the removed client mock-data mode.

## Signed releases and Reviden Store

See [REVIDEN-RELEASE.md](REVIDEN-RELEASE.md) for the signed test APK, tag-triggered hosting workflow, seven GitHub secrets, release channels and client-initiated loan testing.

## Product data and keyboard handling

The product catalogue is fetched from the backend's `GET /api/v1/products` endpoint through `RemoteProductRepository`. There is no bundled product list in the app; names, rates, amounts and periods come from the API.

All navigation screens share IME padding, and all editable fields use a focus-aware scroll request. Forms already use scroll containers, so the viewport shrinks above the keyboard and the focused field scrolls into view. The main bottom navigation hides while the keyboard is open. Source files use UTF-8; release-tool checks reject the malformed punctuation seen in earlier builds.

`app/src/androidTest/java/com/trustbank/loanapp/KeyboardVisibilityTest.kt` checks a multiline field against the actual software keyboard. The new keyboard behavior still needs device testing; the local Android build could not finish because dependency downloads failed. No test app was installed on the connected phone.

## Image previews and action labels

Selected JPEG/PNG files show thumbnails in the document step and application review. Tap a thumbnail to open a larger view with pinch zoom and a reset control. Application details include a Preview image action for uploaded JPEG/PNG documents; it downloads from the authenticated `/api/v1/documents/{id}/content` endpoint. PDFs show their filename rather than an image preview.

Image reads remain capped at 5 MiB, decoded images are sampled to bound memory, and camera EXIF rotation is respected. Preview data stays in memory. Remote previews show loading/errors and allow retry without changing the application.

Submit application and Finish uploading each use a full-width button. Shared button labels use the smaller label style, grow vertically when needed and retain a minimum 48 dp touch height. Review details wrap in separate label/value columns. These mobile changes were not built or run on a device locally, following the user's request to build and test on their end. The new stream-limit unit tests run with the existing Android CI job.
