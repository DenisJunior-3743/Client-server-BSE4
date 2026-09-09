# Validation reference

Every form field in the app is validated client-side (there's no backend yet — see
`README.md`). This document is a map of *where* each rule lives, for explaining the
code. All paths are relative to this branch's root (`app/src/main/java/com/trustbank/loanapp/...`).

## 1. The two-layer architecture

Every field combines two independent layers:

1. **Sanitize-as-you-type** — for fields where certain characters should never be
   typeable at all (names, phone numbers, digits-only amounts, National ID). The
   character is filtered out of the string *before* it ever reaches state, and a
   transient hint names the exact character that was rejected.
2. **Touched/error reveal** — for "is this value acceptable" checks (required,
   format, length, ranges) that can't be enforced by blocking keystrokes (e.g. "must
   be at least 10 characters"). The error only shows once the user has interacted
   with the field, so an empty form doesn't flash red immediately on open.

### Layer 1 — sanitize-as-you-type

- `util/Validators.kt:74-93` — `sanitizeWithFeedback(raw, maxLength, allowedDescription, isAllowed)`.
  The core primitive: filters `raw` down to characters `isAllowed` accepts, and
  returns `(sanitizedString, rejectionMessage?)`. The message names the specific
  rejected character(s), e.g. `"3" is not allowed — letters and spaces only`.
- `ui/components/AppInputs.kt:67-82` — `rememberDismissingHint()`. Holds that
  rejection message as transient UI state that clears itself ~2.2s after the last
  rejection (or immediately, on the next keystroke that isn't rejected).
- Every field built on top of this pattern calls `sanitizeWithFeedback` inside its
  own `onValueChange`, then passes the sanitized value onward and the message into
  `hint`:
  - `NameField` — `ui/components/AppInputs.kt:279-309` (letters + spaces only)
  - `PhoneField` — `ui/components/AppInputs.kt:318-349` (digits only, capped at
    `Validators.PHONE_MAX_LENGTH` = 10)
  - `DigitsField` — `ui/components/AppInputs.kt:356-389` (digits only, optional
    `maxLength`)
  - `NationalIdField` — `ui/components/AppInputs.kt:395-425` (letters/digits only,
    upper-cased, capped at `Validators.NATIONAL_ID_LENGTH` = 14)
- `AppTextField` (`ui/components/AppInputs.kt:84-158`) is what actually renders the
  hint vs. the error: `hint` (amber, `AppColors.Warning700`, line 141-148) takes
  visual priority over `error` (red, `AppColors.Danger600`, line 149-155), since the
  hint is the fresher signal.

### Layer 2 — touched/error reveal

- `ui/common/FormField.kt:21-56` — the `FieldState` class. `value`/`touched`/`error`
  are all `mutableStateOf`; `error` (line 31-32) only returns the real validation
  result once `touched` is true.
  - `onValueChange` (line 34-38) — runs on every keystroke, marks touched.
  - `onFocusLost` (line 40-43) — reveals the error the first time the user leaves
    the field without ever typing in it.
  - `revalidate()` (line 46-48) — re-runs validation only if already touched; used
    for cross-field rules (e.g. re-checking "confirm password" when "password"
    changes).
  - `validateForSubmit()` (line 51-55) — force-touches and returns whether it's
    valid; every screen's submit handler calls this on all its fields so pressing
    Continue/Submit reveals every remaining problem at once.
- `ui/common/FormField.kt:58-61` — `rememberFieldState(initialValue, validate)`, the
  `@Composable` constructor screens call to create one.

## 2. Core validation rules (`util/Validators.kt`)

| Rule | Location | What it checks |
|---|---|---|
| `PHONE_PATTERN` / `isValidUgandaPhone` | `Validators.kt:10,21` | Exactly `07` + 8 digits |
| `sanitizePhoneInput` | `Validators.kt:19` | Strips non-digits, caps at 10 (superseded in the UI by `sanitizeWithFeedback`, kept for reuse) |
| `EMAIL_PATTERN` / `isValidEmail` | `Validators.kt:23-24` | Standard `local@domain.tld` shape |
| `isRequired` | `Validators.kt:26` | Non-blank after trim |
| `isStrongPassword` | `Validators.kt:30-35` | 8+ chars, upper, lower, digit, **and a symbol** |
| `passwordStrengthPercent` / `passwordStrengthLabel` | `Validators.kt:41-58` | 0–100 score across the same 5 criteria, Weak/Fair/Good/Strong labels — drives the strength meter |
| `isAdult` | `Validators.kt:60-61` | `dateOfBirth` is 18+ years before today |
| `parseAmount` | `Validators.kt:63` | Parses a UGX amount string to `Double?` |
| `sanitizeWithFeedback` | `Validators.kt:74-93` | Generic keystroke filter + rejection message (see §1) |
| `sanitizeNameInput` / `isValidFullName` | `Validators.kt:99,101` | Letters+spaces only; trimmed length ≥ 2 |
| `isValidNationalId` | `Validators.kt:107` | Trimmed length == 14 (`NATIONAL_ID_LENGTH`) |

## 3. Screen-by-screen

### Login — `ui/screens/auth/LoginScreen.kt`
| Field | Rule | Line |
|---|---|---|
| Email address | required, valid email format | `LoginScreen.kt:52-58` |
| Password | required only (no strength gate on *login* — see §4) | `LoginScreen.kt:59` |

Submit handler: `LoginScreen.kt:61-67` — both `validateForSubmit()`'d before calling
`viewModel.login`.

### Register — `ui/screens/auth/RegisterScreen.kt`
| Field | Rule | Line |
|---|---|---|
| Full name | required, `isValidFullName`; rendered via `NameField` (letters/spaces only as typed) | `RegisterScreen.kt:48-54`, field at `:118-126` |
| Email address | required, valid email format | `RegisterScreen.kt:55-61` |
| Phone number | required, `isValidUgandaPhone`; rendered via `PhoneField` (digits only as typed) | `RegisterScreen.kt:62-68`, field at `:139-144` |
| Password | required, `isStrongPassword`; strength meter shown live | `RegisterScreen.kt:69-75`, field at `:146-157` (`showStrengthMeter = true`) |
| Confirm password | must equal `password.value`; re-validated via `revalidate()` when password changes | `RegisterScreen.kt:76`, wiring at `:149-152` |

Submit handler: `RegisterScreen.kt:78-89`.

### Settings (account details) — `ui/screens/settings/SettingsScreen.kt`
| Field | Rule | Line |
|---|---|---|
| Full name | required, `isValidFullName`; `NameField` | `SettingsScreen.kt:42-48`, field at `:84-91` |
| Email address | required, valid email format | `SettingsScreen.kt:49-55` |
| Phone number | required, `isValidUgandaPhone`; `PhoneField` | `SettingsScreen.kt:56-62`, field at `:101-106` |

Submit handler: `SettingsScreen.kt:64-71`.

### Profile — edit KYC details — `ui/screens/profile/ProfileScreen.kt`
| Field | Rule | Line |
|---|---|---|
| National ID | required, `isValidNationalId` (14 chars); `NationalIdField` | `ProfileScreen.kt:133-139`, field at `:191-197` |
| Date of birth | required, parseable `YYYY-MM-DD`, `isAdult`; `DateField` (native date picker, see §4) | `ProfileScreen.kt:140-152`, field at `:198-204` |
| Employer | required | `ProfileScreen.kt:153` |
| Job title | required | `ProfileScreen.kt:154` |
| Employment type | dropdown selection (no separate error state — always has a value) | `ProfileScreen.kt:155`, field at `:221-228` |
| Monthly income | digits-only entry (`DigitsField`), then parsed and must be `> 0` | `ProfileScreen.kt:156-159`, field at `:229-236` |
| District | required; `NameField` (letters/spaces — Ugandan district names) | `ProfileScreen.kt:160`, field at `:237-244` |
| Address | required (free text — can contain digits/punctuation, e.g. plot numbers) | `ProfileScreen.kt:161` |

Submit handler: `ProfileScreen.kt:163-188`.

### New loan application — `ui/screens/apply/NewApplicationScreen.kt`
A 5-phase wizard (`STEP_TITLES`, line 69). Each phase's Continue handler calls
`validateForSubmit()` on every field in that phase before advancing
(`ApplicationFormSteps`, lines 200-293).

**Phase 1 — Applicant & Business** (`StepApplicantDetails`, line 319)
| Field | Rule | Line |
|---|---|---|
| Full name | required, `isValidFullName`; `NameField`, pre-filled from session | `NewApplicationScreen.kt:137-143` |
| Date of birth | required, parseable, `isAdult`; `DateField`, pre-filled from profile | `NewApplicationScreen.kt:144-156` |
| NIN | required, `isValidNationalId`; `NationalIdField`, pre-filled from profile | `NewApplicationScreen.kt:157-163` |
| Physical location | required (free text) | `NewApplicationScreen.kt:164` |
| Business name | required (free text) | `NewApplicationScreen.kt:165` |
| Business location | required (free text) | `NewApplicationScreen.kt:166` |

Prefill source: `NewApplicationViewModel.kt:47-60` (`loadProduct`) reads
`AppContainer.session.currentUser` and `AppContainer.profileRepository.getProfile()`.

**Phase 2 — Collateral & Witnesses** (`StepCollateralAndWitnesses`, line 386)
| Field | Rule | Line |
|---|---|---|
| Collateral security | required, ≥ 10 chars after trim | `NewApplicationScreen.kt:169-171` |
| Witnesses (min. 2) — Name | required, `isValidFullName`; `NameField` | `WitnessFields.name`, `NewApplicationScreen.kt:111-117` |
| Witnesses (min. 2) — Contact | required, `isValidUgandaPhone`; `PhoneField` | `WitnessFields.contact`, `NewApplicationScreen.kt:118-124` |

The witness list starts at 2 entries and can only grow (`+ Add another witness`,
line 439-452); the Remove button only appears once there are more than
`MIN_WITNESSES` (line 415-419), so the minimum of 2 can never be violated through
the UI. Continue handler validates every witness's both fields:
`NewApplicationScreen.kt:224-227`.

**Phase 3 — Loan Terms** (`StepLoanTerms`, line 462)
| Field | Rule | Line |
|---|---|---|
| Amount requested | digits-only entry; parsed and range-checked against the selected product's min/max | `NewApplicationScreen.kt:175-183` |
| Period (months) | digits-only entry (max 3 digits); range-checked against product's min/max term | `NewApplicationScreen.kt:184-192` |
| Terms of payment | dropdown; must be non-null before continuing (touched-tracked manually since it's not a `FieldState`) | `NewApplicationScreen.kt:196-198`, gate at `:238-246` |
| Purpose of loan | required, ≥ 10 chars after trim | `NewApplicationScreen.kt:193-195` |

**Phase 4 — Documents** (`StepDocuments`, line 528) — a checklist, not free text;
`NewApplicationViewModel.validateStepTwo()` (`NewApplicationViewModel.kt:70-75`)
requires all of `REQUIRED_APPLICATION_DOCS` (`NewApplicationViewModel.kt:18`) to be
checked.

**Phase 5 — Review & Submit** (`StepReview`, line 576) — read-only summary of every
field above; no new validation, just display. Submit wiring:
`NewApplicationScreen.kt:273-290` → `NewApplicationViewModel.submit(...)`
(`NewApplicationViewModel.kt:81-121`).

### Application detail — respond to info request — `ui/screens/applications/ApplicationDetailScreen.kt`
| Field | Rule | Line |
|---|---|---|
| Your response | required, ≥ 10 chars after trim | `ApplicationDetailScreen.kt:50-52` |

Submit handler: `ApplicationDetailScreen.kt:134-139` (inline in the composable).

## 4. Password strength meter & visibility toggle

- `PasswordField` (`ui/components/AppInputs.kt:160-197`) wraps `AppTextField` with a
  show/hide toggle (`trailingIcon`, lines 183-190) and an optional
  `showStrengthMeter` flag (line 168, rendered at 193-195).
- `PasswordStrengthMeter` (`ui/components/AppInputs.kt:205-253`) reads
  `Validators.passwordStrengthPercent`/`passwordStrengthLabel` and renders a
  color-coded progress bar plus a 5-item checklist (8+ chars / upper / lower /
  number / symbol), built by `PasswordCriterionRow` (lines 255-271).
- Wired in with `showStrengthMeter = true` on: Register's password field
  (`RegisterScreen.kt:156`) and Login's password field (`LoginScreen.kt:124`).
  Login intentionally has **no** strength *requirement* (§3 above) — the meter
  there is informational only, matching that you can't retroactively demand an
  existing account's password meet today's policy.

## 5. Login/registration acceptance (not client-side validation, but related)

There's no backend yet (`README.md`), so `FakeAuthRepository.login`
(`data/repository/AuthRepository.kt:18-25`) doesn't check credentials against a
real account store — it only requires the two fields to be non-blank (the *format*
checks already happened in the screen before this is ever called), then signs in as
the single demo applicant using whatever email was typed.
