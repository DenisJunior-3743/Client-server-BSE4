# TrustBank Loans progress

Updated: 2026-11-13 (Africa/Kampala).

## Repository merge

- Imported the React dashboard into `web/` and the bundle's mobile API changes into the `mobile/app/` project.
- Existing `backend/` was identical to the bundle; retained it and added targeted protection/tests.
- Kept Android visuals, Reviden updates, package/version identity, signing setup and existing local files.
- Added Gradle wrapper scripts/jar; pinned Gradle 8.6 for Android plugin 8.4.2. Added Maven 3.9.9 wrapper.
- Made smoke-test borrower phone numbers unique per timestamp; Bash syntax validation passes.
- Excluded dependency caches, build outputs, local environment files and signing credentials.
- Added `docs/Tasks.pdf`, `docs/TASKS-COVERAGE.md`, and current combined-repository instructions.
- Hardened payload protection to check actual bytes even without Content-Length. Reads are bounded to limit + 1 bytes.

## Verified in this session

- Backend: all 83 Java source files compiled with Java 21 and real Spring Boot dependencies.
- Backend: **18 tests passed**, zero failures/errors. Coverage: repayment mathematics, workflow reachability/transitions, cached retry replay, changed payload refusal, stale officer versions, exact OTP/offer expiry, body/rate protection, and HTTP 503 mapping for PostgreSQL lock contention and transient database connection failures.
- Android: `:app:assembleDebug :app:testDebugUnitTest` completed successfully with Java 21 and Gradle 8.6. Kotlin/Java compilation and APK packaging passed; the unit-test task reports NO-SOURCE (there are no Android unit tests). The local Java Unix-socket issue needed the shell-only workaround documented in `DEPLOYMENT.md`.
- Web: `npm ci`, `npm run build`, `npm run lint` succeeded. Two non-blocking React lint warnings remain (AuthContext exports and React Hook Form compatibility); the production bundle also has a size warning.

## Neon and hosting setup

- Moved real credentials out of `.env.example` into ignored `backend/.env`; generated a private JWT signing secret. The Git staging scan excludes both secrets.
- Added a local PowerShell environment loader and a cross-platform Python API smoke test.
- Configured direct JDBC access to Neon and applied both Flyway migrations to PostgreSQL 18.7. A separate migration connection avoids the request pool's idle timeout during schema setup.
- The running API reports UP, Swagger UI returns HTTP 200, and OpenAPI generates 25 paths.
- The full Python smoke test passes against Neon: registration/OTP/login, KYC, concurrent duplicate submissions, cached replies, changed-payload refusal, background credit checks, concurrent approvals (one 200 and one 409), offer acceptance, disbursement and repayment replay, role separation, persisted notifications and audit records. Development test records remain in the database.
- Fixed Hibernate's PostgreSQL lock-timeout mapping to return retryable HTTP 503 rather than HTTP 500; both regression tests pass.
- Added Vercel Container preset configuration and deployment instructions. Vercel's published JSON schema accepts this preset. Scheduled jobs are not reliable while a container is scaled down.
- Flyway 11.7.2 emits a PostgreSQL 18 compatibility warning; the migrations applied and validated successfully, but upgrading to a version certified for 18 is still recommended before production.

## Pending

- Verify both clients end to end against the real API and verify hosted production TLS.
- Configure the Vercel project and its private production environment variables; image build and deployment have not been run locally.
- Durable background processing, reliable hosted scheduling, and rollback/crash tests remain future checks.

The implementation for the six coursework concepts is present. The unit and Neon API checks cover database races, but are not proof of deployed TLS, crash recovery or full client/API interoperability. See `docs/TASKS-COVERAGE.md` for evidence and remaining checks, `DEPLOYMENT.md` for environment setup, and `docs/MOBILE.md` for known mobile limitations.

## Learning layout and component branches

- Android source, Gradle files, SDK settings and signing material now live under `mobile/`; React stays in `web/` and Spring Boot in `backend/`.
- `main` is the complete monorepo. `web`, `mobile` and `backend` are folder-only snapshots with component-specific CI; existing histories are preserved with forward commits.
- Restored the old main branch's diagrams, project plan and reference documents to the monorepo.
- Updated build paths and Vercel branch/root-directory instructions. Real Neon credentials and Android signing files remain ignored.
- See `docs/REPOSITORY-LAYOUT.md` for safe folder synchronization between the focused branches and main.

- Relocated Android verification passed from `mobile/`: debug APK built successfully with the existing Gradle 9.7.1 wrapper; the unit-test task reports NO-SOURCE. Backend and React source paths are unchanged.

## Component merge repair

- Restored backend/, mobile/, shared documentation and diagrams after the web PR applied the folder-only snapshot's deletions to main.
- Integrated the initial backend and mobile snapshot histories while retaining the complete monorepo. Component source trees match their focused branches; no application source was discarded.
- Updated the branch guide: future commits on the existing component branches can use ordinary pull requests into main. Refresh focused branches from main by copying only the component folder.
- Fixed Android CI setup to install explicit current SDK packages rather than the removed tools package, and matched the verified Java 21 runtime. Prior backend and web CI checks passed; Android had failed before source compilation.

## GridFS documents and initial admin (2026-11-13)

The backend accepts real multipart PDF/JPEG/PNG uploads, stores file bytes in Atlas GridFS, and keeps private GridFS references in Neon via Flyway V3. Owner/staff downloads require JWT authentication. The mobile app now selects and uploads files during application submission and from application detail; staff can download them from the web dashboard. Credit checks require real files and verification of the latest upload of each required type.

The initial admin defaults to `admin@bank.com` / `Admin123`, stores a BCrypt hash, and is created once without resetting existing passwords. Staff registration remains admin-only. Automatic sample staff seeding is disabled by default. Atlas secrets stay in ignored `atlas-credentials.env` and `backend/.env`. Deployment variables and setup are documented in `backend/GRIDFS-SETUP.md` (or `GRIDFS-SETUP.md` from the backend folder).

Validation: 33 backend unit tests passed; the opt-in Atlas upload/read/delete check passed separately. The packaged API passed health/OpenAPI checks and the full Neon/GridFS workflow smoke test, including admin-created staff, real document uploads/downloads, credit checks, concurrent approvals, disbursement and repayments. The web production build and lint passed. Live smoke-test development records remain in the databases.

## API-only apps, staff modules and CSS warnings (2026-11-14)

Web and mobile client mocks/fake repositories have been removed. Web sign-in always authenticates against the API, rejects tokenless saved demo sessions and reports load failures instead of substituting empty queues. Mobile profile edits now call the account API. Android debug defaults to the emulator's local backend URL; deployed/physical-device builds accept `apiBaseUrl` or `API_BASE_URL`.

One staff-role policy filters the sidebar and guards module URLs. Admin sees all four modules and the bank overview; each other staff role sees its own workspace. Shared loan detail/profile pages remain available to staff.

The Firefox CSS warning came from Recharts CartesianGrid measuring text with undefined font/letter-spacing values. Grid coordinates now use scaled ticks via public coordinate-generator callbacks. An isolated browser reproduced 16 invalid assignments in the previous grid and zero in the fixed charts across all staff roles, while checking rendered grid lines and forbidden URL redirects. Five role-policy tests pass, as do the web build/lint and Android debug build. Android has no unit-test sources. API error presentation and invalid sign-in were checked in the browser.

Android GitHub Actions already exists at repository-root `.github/workflows/verify.yml` in main and mobile branches; the most recent mobile run before this change passed. Web role-policy tests are now included in CI.

## Signed releases and Reviden automation (2026-11-14)

The prior main/web/mobile updates were already pushed and their GitHub checks passed. Added the supplied Reviden Store reference documents and adapted the FarmerStack tag/manual release workflow for the Kotlin mobile folder. Release codes are allocated above both store channels and the distributed-code floor. Upload, storage sync and explicit publication are separate verified phases; no management token enters the APK.

The private local release key matches production 1.1.1 (code 2), verified against the existing downloaded APK. The public fingerprint is pinned. Gradle now accepts CI versions/signing, uses the supplied HTTPS backend, and embeds the appropriate Reviden update channel. The signed/shrunk 1.2.0-beta.1 testing APK (code 3), SHA-256, JSON metadata and R8 mapping are collected under ignored mobile/releases. Thirteen offline release-script tests, Bash syntax checks and release workflow YAML validation passed. Local release compilation and vital lint passed; Android unit test tasks have no sources.

GitHub release secrets were not configured during setup. The hosted backend health endpoint returned HTTP 500 and later timed out, so real loan-flow testing is blocked on that deployment. Publication requires a healthy backend after preserving the signed artifact. Full secret values, tag commands and borrower-initiated testing instructions are in mobile/REVIDEN-RELEASE.md. No new store release has been published during setup.

## Mobile punctuation and keyboard visibility (2026-11-14)

Repaired malformed UTF-8 punctuation in the application steps, amount/period ranges, witness summaries and update messages. Added editor encoding settings and a source-encoding regression check. Confirmed the product catalogue comes from `GET /api/v1/products` through `RemoteProductRepository`, with no hardcoded mobile products.

Shared navigation now applies keyboard insets; editable fields request scrolling when focused and when keyboard height changes. Existing form scroll containers remain in use, and main bottom navigation hides during typing. Added an instrumented software-keyboard visibility test.

Validation: all 14 offline release/encoding tests passed and `git diff --check` passed. Android compilation/device verification is incomplete: dependency resolution failed against Maven/Google, and the user requested that local builds stop so they can build and test. No app was installed and no new release tag was created.

## Staff editing and deletion (2026-11-14)

Added admin-only GET/PUT/DELETE `/api/v1/staff/{id}` endpoints and OpenAPI summaries. Updates accept name, email, phone, staff role and branch without changing passwords. Deletion removes access and the active directory entry through the existing active flag, preserving all loan/audit references and reserving the email. Staff changes generate audit events. JWT authentication now loads current account state so deletion and role changes affect existing tokens on subsequent requests. Administrator row locks and caller checks protect against concurrent admin removal; self-deletion and self-demotion are rejected. Bootstrap no longer recreates a renamed/deleted initial admin when another admin identity exists. No migration or new environment variable is needed.

The web staff table now includes accessible pencil/trash actions, a prefilled edit page, delete confirmation, pending/error feedback and own-account protection. The existing staff creation flow still shows its one-time password.

Validation: 53 backend tests passed, with the opt-in Atlas integration test skipped. These cover endpoint authorization/validation, staff mutations, password preservation, audit events, session revocation and bootstrap behavior. Web type checking, lint and five access-policy tests passed (lint has two existing warnings). An isolated browser with API responses intercepted verified prefilling/saving, edit errors, delete cancellation/error/success, self protection and a clean new-account form with no runtime errors. Live staff records were not changed. No release/APK build or tag was created.

## Staff credentials and password changes (2026-11-14)

Added admin-only PUT `/api/v1/staff/{id}/credentials` accepting only a new password; the account's email remains its username. Staff creation accepts an optional chosen initial password, otherwise retaining generated temporary credentials. Passwords are BCrypt hashed, validated against the existing strength rule and a 72-byte UTF-8 ceiling, and excluded from audit details and password-change responses.

The web staff list has a key icon opening a credentials form with a read-only email, new password and confirmation. Staff creation supports an optional initial password. Successful own-account password changes sign the admin out. Flyway V4 adds credential-version counters; JWTs from before a password change fail on subsequent requests. Version-zero compatibility preserves existing tokens until a password change. Self-service password changes use the same revocation counter. No new environment variable is required; V4 applies when the backend starts on deployment.

Validation: 64 backend tests passed, with the opt-in Atlas test skipped. Tests verify BCrypt password replacement, legacy/new JWT handling, session revocation, unchanged username/details, permissions, strength validation, and both initial-password options. Web type checking, lint (existing warnings only) and five access-policy tests passed. Isolated browser checks verified read-only usernames, strength/confirmation errors, backend errors, password-only payloads, chosen/generated creation and own-reset logout. No live passwords were changed and no release/APK build or tag was created.

## Password visibility, image previews and clipped actions (2026-11-14)

The web shared TextField now adds an independent accessible eye toggle to every password field: login, initial staff password, new password and confirmation. Controls retain field values/focus, support keyboard activation, reserve icon space and do not submit the form. Password controls have a usable minimum width for narrow containers.

Mobile selected JPEG/PNG documents now show thumbnails in the picker and review, with a larger pinch-zoom view. Existing uploaded images can be fetched on request through the authenticated content endpoint. Native decoding is sampled and respects EXIF orientation; capped streams and memory-only previews avoid unbounded reads and public file URLs. Remote previews have loading/error/retry controls. PDFs retain filename display. New pure unit tests cover stream limits.

Submit application and Finish uploading now use full-width actions. Shared primary/secondary buttons use smaller labels and a minimum rather than fixed height so wrapped text remains visible. Review label/value columns have separate weighted widths and a gap.

Validation: web type checking, lint (existing warnings only), and five access-policy tests passed. Isolated browser checks verified all four visibility controls, keyboard use, value/focus retention, narrow-width alignment, absence of unintended requests, and the existing credentials create/reset flows. All 14 offline mobile release/encoding checks passed. No local Android compilation, instrumented test, APK build or release tag was performed, following the user's build preference; new Android unit tests are committed for CI. No backend source or storage configuration was changed.
