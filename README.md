# TrustBank Loans

Online bank loan processing coursework: Kotlin/Jetpack Compose for borrowers, React/TypeScript for staff, Spring Boot REST API, and PostgreSQL (Neon for deployment).

## Repository layout

```text
app/                 Android borrower app
build.gradle.kts     Android project (open this repository in Android Studio)
gradle/, gradlew*    Gradle 8.6 wrapper
web/                 React staff dashboard
backend/             Spring Boot API, migrations, tests and deployment files
docs/                Requirements mapping and mobile guide
```

The Android project stays at the repository root to preserve its existing package name, signing setup and Reviden update integration. Do not open the old `mobile/` cache folder as a second app. All three components now live together; switching branches or applying the original bundle patches is unnecessary.

## Build before database setup

Use Java 21 for the backend and Java 17 or 21 for Android. The checked-in Maven wrapper downloads Maven 3.9.9 on first use. The Android SDK path belongs in ignored `local.properties`.

```powershell
# From the repository root
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
cd web
npm ci
npm run build
npm run lint
cd ..\backend
.\mvnw.cmd test
```

These checks do not require a configured database. Backend unit tests cover repayment mathematics, legal workflow steps, retry reply caching, changed-payload refusal, stale versions, OTP/offer expiry and request limits. PostgreSQL concurrency and end-to-end tests are the next stage.

## Connect the clients after configuring PostgreSQL

Set the backend's environment variables using `backend/.env.example` as a reference. Spring Boot does **not** automatically load that file; export the values in your shell or run configuration. Follow [DEPLOYMENT.md](DEPLOYMENT.md) for PowerShell commands.

- Start the API from `backend/`: `.\mvnw.cmd spring-boot:run`.
- In `web/`, set `VITE_API_URL=http://localhost:8080` in an ignored `.env`, then run `npm run dev`.
- Build Android from the root: `.\gradlew.bat installDebug -PapiBaseUrl=http://10.0.2.2:8080/` (emulator). Use your PC's LAN address for a physical phone.
- Without an API URL, either client uses demo data. Debug Android builds allow local HTTP; releases require HTTPS.

See [docs/TASKS-COVERAGE.md](docs/TASKS-COVERAGE.md) for the six PDF requirements and implementation paths, [PROGRESS.md](PROGRESS.md) for verified status, and [docs/MOBILE.md](docs/MOBILE.md) for mobile behavior. Signing files, environment secrets, build outputs and dependency caches are ignored.

## Understand the backend

Start with the [Spring Boot beginner guide](docs/SPRING-BOOT-GUIDE.md) for the architecture, data flow, transactions, security and the current Swagger/OpenAPI setup.

For Vercel hosting, use the [backend deployment guide](docs/VERCEL-DEPLOYMENT.md). Real database credentials belong in ignored `backend/.env`, not `.env.example`.
