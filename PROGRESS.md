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
