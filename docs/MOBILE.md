# Android borrower app

Open `mobile/` in Android Studio. `mobile/app/` is the only Android application module; the Gradle wrapper uses 9.7.1 with Android plugin 8.4.2 and Kotlin 1.9.24.

By default the app uses in-memory demo repositories. Any valid email and nonempty password signs into the demo account. For real authentication and data, build from `mobile/` with:

```powershell
.\gradlew.bat installDebug -PapiBaseUrl=http://10.0.2.2:8080/
```

`AppContainer` selects Retrofit repositories when that URL is present. The API path is `/api/v1`. JWTs are attached to each request. Loan submissions, offer acceptance and repayments carry an Idempotency-Key reused on retries. Registration opens phone verification; the server decides whether OTPs and offers have expired. Repayment schedules come from server calculations.

The app preserves input validation, navy/gold styling, amount visibility, package name `com.trustbank.loanapp`, version 1.1.1 (code 2), signing configuration and Reviden updates. See `mobile/VALIDATION.md` for input rules and `mobile/integration.md` for APK updates.

Current limitations: JWTs and pending verification are in memory only; restarting after registration requires a recovery flow for an unverified login. Document files now upload through the authenticated API into MongoDB Atlas GridFS; see [the GridFS setup guide](../backend/GRIDFS-SETUP.md). Credit bureau, SMS delivery and payment settlement are demo integrations. The server records payments; it does not charge a real mobile money account.
