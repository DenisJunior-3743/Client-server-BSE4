# Demo Accounts

Task 1 has no real backend or auth — the login form checks against the staff list in `src/mocks/data.ts`. Password is the same for every account: **`Passw0rd1`** (defined as `DEMO_PASSWORD` in that file).

| Email | Role | Branch |
|---|---|---|
| grace.officer@bankloan.ug | Loan Officer | Mbarara Branch |
| ronald.officer@bankloan.ug | Loan Officer | Kampala Branch |
| sarah.analyst@bankloan.ug | Credit Analyst | Mbarara Branch |
| david.analyst@bankloan.ug | Credit Analyst | Kampala Branch |
| esther.manager@bankloan.ug | Branch Manager | Mbarara Branch |
| peter.manager@bankloan.ug | Branch Manager | Kampala Branch |
| admin@bankloan.ug | Admin | Head Office |

Any staff created through **Admin → Staff Management → Add staff** during the session also signs in with the same demo password (see `createStaff` in `src/api/staff.ts`) — it's just not persisted past a page reload.
