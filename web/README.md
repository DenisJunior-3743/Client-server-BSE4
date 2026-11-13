# TrustBank web dashboard

React and TypeScript staff dashboard backed by the Spring Boot API. All sign-in, staff, product, loan and audit data comes from the backend. There is no mock-data fallback or offline demo sign-in.

## Run and deploy

1. Copy `.env.example` to `.env` and set `VITE_API_URL` to your backend origin (for local development, `http://localhost:8080`).
2. Run `npm ci` and `npm run dev`.
3. For Vercel, select root directory `web`, Vite preset and output `dist`. Set `VITE_API_URL` in the web project's environment before building/redeploying.

If the API is not configured or unavailable, the app reports an error instead of signing into sample data. Old stored demo accounts without a JWT are discarded. Use accounts created by the backend; the initial administrator is documented in [the backend setup guide](../backend/GRIDFS-SETUP.md).

## Staff workspaces

| Role | Visible workspaces |
|---|---|
| Admin | All four workspaces and the bank-wide overview |
| Loan officer | Loan Officer |
| Credit analyst | Credit Analyst |
| Branch manager | Branch Manager |

Every staff member can open their profile and shared loan-detail pages. Typing another role's module URL redirects to the signed-in role's overview. Sidebar and route checks use the same policy in `src/lib/access.ts`. Backend authorization continues to govern API operations.

## Charts

Chart grid lines use Recharts' public coordinate-generator callbacks and scaled axis ticks. This avoids the library grid's text-measurement call with undefined font/letter-spacing values, which caused Firefox CSS warnings. Axis labels and tooltips retain their formatting.

## Validation and checks

Input validation and Uganda phone formatting remain in `src/lib` and reusable form controls. `npm test` verifies all four staff roles, unauthorized URLs and borrower/signed-out access. `npm run build` type-checks and creates the production bundle; `npm run lint` runs oxlint. GitHub Actions runs these checks from the repository-root `.github/workflows/verify.yml`.
