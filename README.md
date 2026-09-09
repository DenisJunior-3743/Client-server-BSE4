# TrustBank Loans — Mobile App (Task 1)

Kotlin + Jetpack Compose applicant app for the Bank Loan Application System. Covers the mobile side of Task 1: full UI + client-side validation, running entirely on **in-memory mock data** (`data/mock/MockData.kt`) — no backend yet.

## Getting started

1. Open the `mobile/` folder as a project in **Android Studio** (Koala/2024.1 or newer recommended).
2. Let it sync Gradle. This repo's `gradle-wrapper.properties` points at Gradle 8.6, but `gradle-wrapper.jar` (a binary file) isn't checked in from this environment — Android Studio will offer to regenerate the wrapper automatically on first sync. If it doesn't, run once from a machine with Gradle installed:
   ```
   gradle wrapper --gradle-version 8.6 --distribution-type bin
   ```
3. Run the `app` configuration on an emulator or device (min SDK 26 / Android 8.0+).

## Demo login

- Email: `allan.tumusiime@mail.com`
- Password: `Passw0rd1`

Registering a new account also works — it just signs you in as the same demo applicant (no persistence yet).

## What's implemented

- Splash → Login/Register → bottom-tab shell (Home, Products, Applications, Profile)
- Product list/detail → 3-step new application flow (loan details → documents → review & submit), each step validated before you can continue
- Application list + detail with a document checklist and a respond-to-info-request form
- KYC profile view/edit and account settings, both with the same validation rules as the web dashboard
- Client-side validation throughout: required fields, email format, Uganda phone format (see below), password strength, DOB age check (18+), amount/term bounds against the selected product

## Uganda phone number rule

`PhoneField` (`ui/components/AppInputs.kt`) and `Validators.sanitizePhoneInput` (`util/Validators.kt`) strip every non-digit character and cap the result at 10 characters **inside `onValueChange`** — so a letter or symbol never appears in the field, and typing an 11th digit is simply ignored. Final format required: `07XXXXXXXX`. This mirrors `web/src/components/ui/PhoneField.tsx` and `web/src/lib/phone.ts` exactly.

## Design system

Colors in `ui/theme/Color.kt` are the same hex values as the web dashboard's `web/src/index.css` tokens — kept in sync by hand since the two clients don't share a build step. If you change one palette, update the other.

## Known limitations (by design, for Task 1)

- No real backend/API calls, no persistence beyond the current app process, no authentication token.
- No real file upload — the document step is a confirmation checklist, not a file picker.
- Not built/run in this environment (no Android SDK here) — verify in Android Studio before relying on it.
