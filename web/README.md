# TrustBank Loans — Web Dashboard (Task 1)

React + TypeScript staff dashboard for the Bank Loan Application System. Covers the web side of Task 1: full UI + client-side validation, running entirely on **in-memory mock data** (`src/mocks/data.ts`) — no backend yet.

## Getting started

```
npm install
npm run dev
```

Then open the printed local URL (defaults to http://localhost:5173).

## Demo login

See [DEMO_CREDENTIALS.md](DEMO_CREDENTIALS.md) for the staff accounts and password — kept out of the app itself so the login page stays a real login page.

## Mock mode and API mode

The dashboard runs in one of two modes, chosen by `VITE_API_URL` (see `.env.example`):

- **Not set (default):** the Task 1 in-memory mock data in `src/api/mock`. No backend needed.
- **Set, e.g. `VITE_API_URL=http://localhost:8080`:** every function in `src/api` calls the Spring Boot
  backend through `src/api/http`. Log in with the same demo accounts (the backend's `dev` profile seeds them).

Pages import from `@/api/*` and do not know which mode they are in. In API mode each staff command sends the
`version` of the application it loaded; if a colleague changed it first, the server replies `409 VERSION_CONFLICT`
and the detail page reloads and says so instead of overwriting their work (`isVersionConflict` in `src/api/client.ts`).

## What's implemented

One shared dashboard with a sidebar covering every staff domain (Loan Officer, Credit Analyst, Branch Manager, Admin) — everyone sees all four for now; role-based filtering is a later phase per `docs/PROJECT-PLAN.md`. Each domain's first item is a graphical Overview & Stats page (Recharts). Application detail page has role-aware action panels: document verify/reject, credit assessment, final decision, request-more-info.

## Uganda phone number rule

`PhoneField` (`src/components/ui/PhoneField.tsx`) and `sanitizePhoneInput`/`isValidUgandaPhone` (`src/lib/phone.ts`) strip every non-digit character and cap the result at 10 characters inside the input's `onChange` — so a letter or symbol never appears in the field, and typing an 11th digit is simply ignored. Final format required: `07XXXXXXXX`.

## Design system

Color tokens live in `src/index.css` under `@theme`. The Kotlin mobile app mirrors these exact hex values in `mobile/app/.../ui/theme/Color.kt` — update both if the palette changes.

## Scripts

- `npm run dev` — dev server
- `npm run build` — type-check (`tsc -b`) then production build
- `npm run lint` — oxlint
