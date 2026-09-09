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

- Splash → Onboarding (3 skippable "about the bank" pages, shown whenever a logged-out user cold-starts the app) → Login/Register → bottom-tab shell (Home, Products, Applications, Profile)
- Product list/detail → 3-step new application flow (loan details → documents → review & submit), each step validated before you can continue
- Application list + detail with a document checklist and a respond-to-info-request form
- KYC profile view/edit and account settings, both with the same validation rules as the web dashboard
- Client-side validation throughout: required fields, email format, Uganda phone format (see below), full name/District (letters only), National ID (14-char alphanumeric), loan amount/term/income (digits only), password strength, DOB age check (18+), amount/term bounds against the selected product
- Every character-restricted field (name, phone, digits, National ID) shows a live, specific hint the instant an invalid keystroke is rejected — e.g. typing `D3nis` flashes `"3" is not allowed — letters and spaces only` — instead of just silently dropping it

## Uganda phone number rule

`PhoneField` (`ui/components/AppInputs.kt`) and `Validators.sanitizePhoneInput` (`util/Validators.kt`) strip every non-digit character and cap the result at 10 characters **inside `onValueChange`** — so a letter or symbol never appears in the field, and typing an 11th digit is simply ignored. Final format required: `07XXXXXXXX`. This mirrors `web/src/components/ui/PhoneField.tsx` and `web/src/lib/phone.ts` exactly. The same live-sanitize-with-a-hint pattern (`Validators.sanitizeWithFeedback`) now also backs `NameField` (letters/spaces), `DigitsField` (loan amount/term/income), and `NationalIdField` (uppercase alphanumeric, 14 chars).

## Password policy

`Validators.isStrongPassword` requires 8+ characters with upper case, lower case, a digit, and a symbol. While registering, `PasswordStrengthMeter` shows a color-coded progress bar plus a live checklist of which of those five criteria (length, upper, lower, digit, symbol) are currently met, so the user sees exactly what's missing instead of only a pass/fail message on submit. `PasswordField` also has a show/hide visibility toggle.

## Design system

Colors in `ui/theme/Color.kt` are a Ugandan-bank-inspired palette (navy blue primary + gold accent, pulled from Centenary Bank's public site) and are **deliberately no longer synced** with the web dashboard's `web/src/index.css` tokens — this was a one-off mobile reskin, not a shared-token update. If the web app gets the same treatment later, update it separately.

## Known limitations (by design, for Task 1)

- No real backend/API calls, no persistence beyond the current app process, no authentication token.
- No real file upload — the document step is a confirmation checklist, not a file picker.
- Not built/run in this environment (no Android SDK here) — verify in Android Studio before relying on it.
