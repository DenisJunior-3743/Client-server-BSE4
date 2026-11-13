# Run and deploy TrustBank Loans

The Android app is in `mobile/`, the dashboard is in `web/`, and the API is in `backend/`. No branch switching or bundle patches are needed. Verified checks and remaining work are in `PROGRESS.md`.

## 1. Build without a database

Use Java 21 for the backend; Java 17 or 21 and Android SDK 34 for Android. Set `JAVA_HOME` to the installed JDK and keep the SDK path in ignored `mobile/local.properties`.

```powershell
# Inside mobile/
cd mobile
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
cd web
npm ci
npm run build
npm run lint
cd ..\backend
.\mvnw.cmd test
```

For local credentials already stored in ignored `backend/.env`, use `.\backend\scripts\run-local.ps1` from the repo root (add `-Action Test` or `-Action Package` for those commands). Direct Maven commands still require exported variables.

The Maven wrapper downloads Maven 3.9.9. The checked-in Gradle wrapper uses 9.7.1 for Android plugin 8.4.2. On Unix use `./gradlew` and `./mvnw` (make them executable if necessary).

## 2. Database environment

Create a PostgreSQL database locally, or a Neon project. For Neon use the direct connection host and `sslmode=require`. `backend/.env.example` documents the required values; Spring Boot does **not** automatically load `.env`. Set variables in your shell, IDE run configuration or host environment.

PowerShell, from the repository root:

```powershell
cd backend
$env:DATABASE_URL='jdbc:postgresql://<host>/<database>?sslmode=require'
$env:DATABASE_USER='<user>'
$env:DATABASE_PASSWORD='<password>'
# Generate a local JWT signing secret without printing it.
$jwtBytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
$env:JWT_SECRET=[Convert]::ToBase64String($jwtBytes)
$env:CORS_ALLOWED_ORIGINS='http://localhost:5173'
$env:SPRING_PROFILES_ACTIVE='dev'
.\mvnw.cmd spring-boot:run
```

For local PostgreSQL the URL is `jdbc:postgresql://localhost:5432/loans`; create `loans` first and use your database user's actual password. Never commit credentials.

Flyway automatically applies `backend/src/main/resources/db/migration/V1__init_schema.sql` and `V2__seed_loan_products.sql`. Do not manually recreate tables alongside Flyway.

Check `http://localhost:8080/actuator/health` and `http://localhost:8080/swagger-ui.html`. Development mode seeds staff accounts listed in `web/DEMO_CREDENTIALS.md` and echoes OTPs in API responses for testing. Production mode disables those development behaviors.

## 3. Connect the dashboard

In `web/`, create an ignored `.env` containing:

```dotenv
VITE_API_URL=http://localhost:8080
```

Run `npm run dev` and open `http://localhost:5173`. Without this variable, the dashboard uses mock data. API URLs are embedded at build time; rebuild after changing them for deployment.

## 4. Connect Android

Open `mobile/` in Android Studio, sync, then build inside `mobile/`:

```powershell
.\gradlew.bat installDebug -PapiBaseUrl=http://10.0.2.2:8080/
```

`10.0.2.2` reaches the development PC from an Android emulator. On a physical phone, use the PC's LAN address on the same network. Debug builds allow HTTP; release builds require HTTPS. Without `apiBaseUrl`, the app uses demo repositories.

Register, confirm the OTP, complete the KYC profile and apply for a loan. With a development API the verification code appears in the UI. SMS delivery, credit bureau calls and payment settlement are stubs. Documents contain metadata rather than uploaded files.

## 5. Verify against PostgreSQL

From the repository root, run `python backend/scripts/smoke-test.py` (Python 3, no extra packages). It tests concurrent submissions/approvals and the complete workflow through first repayment.

Alternatively, from `backend/`, in Bash with `curl` and `jq`, run:

```bash
bash scripts/smoke-test.sh http://localhost:8080
```

This creates development test records. It checks registration/verification, duplicate submission, asynchronous credit checks, stale decisions, duplicate disbursement, repayments and role restrictions. Use a development database; each run generates its borrower email and phone from the current timestamp.

Then exercise simultaneous duplicate requests and manager decisions in independent transactions; verify rollback and retry behavior, actual deadline responses, query plans and audit/notification persistence. Run both clients against the real API and walk a loan from submission to closure. See `docs/TASKS-COVERAGE.md` for the full checklist.

## 6. Deployment

Build the backend with `backend/mvnw.cmd package` (or `./mvnw package` on Unix). The executable is `backend/target/loan-backend-0.1.0-SNAPSHOT.jar`; a Dockerfile is also in `backend/`.

Set `SPRING_PROFILES_ACTIVE=prod`, a strong persistent `JWT_SECRET`, real database variables and the dashboard's exact HTTPS origin in `CORS_ALLOWED_ORIGINS`. Terminate TLS with the reverse proxy. Templates for systemd, nginx and environment variables are under `backend/deploy/`. Only trust forwarded headers from your reverse proxy; do not expose the API's internal HTTP port publicly. Production requests require HTTPS.

Build the dashboard with `npm run build` in `web/` using its deployed `VITE_API_URL`, and serve `web/dist/` with SPA fallback. Build Android inside `mobile/` with:

```powershell
.\gradlew.bat assembleRelease -PapiBaseUrl=https://api.<your-domain>/
```

The existing ignored `mobile/keystore/keystore.properties` supplies release signing. Preserve the package name and signing key for updates; increment `versionCode` when publishing a new APK. See `mobile/integration.md` for Reviden Store update behavior.

## Windows Java loopback troubleshooting

If Gradle fails before compilation with `Unable to establish loopback connection` and a UnixDomainSockets `Invalid argument: connect` cause, this machine's Java runtime can use TCP fallback by choosing a nonexistent Unix socket temp directory for this shell only:

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=D:\4_1_no_unix_sockets'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon --console=plain
Remove-Item Env:JAVA_TOOL_OPTIONS
```

Do not create that directory or save this machine-specific workaround in project Gradle settings. CI uses a clean Linux runner.

## Deploy on Vercel

See [docs/VERCEL-DEPLOYMENT.md](docs/VERCEL-DEPLOYMENT.md) for the Container preset, root directory, JDBC environment variables and scheduled-job limitation.
