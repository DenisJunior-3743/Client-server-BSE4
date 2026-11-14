# TrustBank Loans: Backend API

Spring Boot REST API for the Bank Loan Application System (Client Server Programming, BSE4).
It serves the Kotlin mobile app (borrowers) and the React dashboard (bank staff), and stores
account and loan records in PostgreSQL on Neon. Document bytes are stored privately in MongoDB Atlas GridFS.

Progress, the course-concept mapping and next steps are in [PROGRESS.md](PROGRESS.md).

## Stack

| Layer | Technology |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5 (Web, Security, Data JPA, Validation, Actuator) |
| Database | PostgreSQL 16+ (Neon), schema owned by Flyway |
| Document files | MongoDB Atlas GridFS; accessed through authenticated API endpoints |
| Auth | JWT (HS256, jjwt), BCrypt password hashes |
| API description | OpenAPI via springdoc (`/swagger-ui.html`) |
| Build | Maven |

## Folder and branch

This project is in `backend/` on the complete `main` monorepo and the focused `backend` branch. Enter that folder before running Maven. For the beginner architecture and Swagger guide, see the [guide on main](https://github.com/DenisJunior-3743/Client-server-BSE4/blob/main/docs/SPRING-BOOT-GUIDE.md).

## Run it locally

If your private `backend/.env` is already configured, run `.\scripts\run-local.ps1` from this folder. Otherwise follow these steps:

1. Create a database. Either a Neon project, or local PostgreSQL:
   ```
   createdb loans
   ```
2. Set the connection (see `.env.example`). For Neon use the **direct** host, not the `-pooler` one:
   ```
   export DATABASE_URL='jdbc:postgresql://<neon-host>/neondb?sslmode=require'
   export DATABASE_USER=<user>
   export DATABASE_PASSWORD=<password>
   export JWT_SECRET="$(openssl rand -base64 48)"
   ```
3. Start:
   ```
   mvn spring-boot:run
   ```
   Flyway creates/updates the tables on startup. The initial admin is `admin@bank.com` / `Admin123`.
   It is created once with a BCrypt password hash; restarts never reset an existing account.
   The admin creates staff from the dashboard or `POST /api/v1/staff`. Sample staff seeding is off
   by default; explicitly set `SEED_DEMO_DATA=true` only if you need the old demo staff.
   The `dev` profile returns OTP codes so you can test without an SMS gateway.

4. Open http://localhost:8080/swagger-ui.html, or point the dashboard at it:
   ```
   # in the web project
   echo 'VITE_API_URL=http://localhost:8080' > .env
   npm run dev
   ```

For a real deployment run with `SPRING_PROFILES_ACTIVE=prod` (HTTPS required, no demo data, no
OTP echo) and set `CORS_ALLOWED_ORIGINS` to the dashboard's URL.

## Endpoints (all under `/api/v1`)

| Method and path | Who | Notes |
|---|---|---|
| `POST /auth/register` | public | Borrower sign-up; sends a 6-digit code (5 min firm deadline) |
| `POST /auth/otp/verify`, `POST /auth/otp/resend` | public | 410 if the code has expired |
| `POST /auth/login` | public | Returns the JWT access token |
| `GET/PUT /me`, `PUT /me/password` | any signed-in user | |
| `GET/PUT /me/profile` | BORROWER | KYC profile |
| `GET /me/notifications`, `POST /me/notifications/{id}/read` | any | |
| `GET /products` | any | `POST /products` is ADMIN only |
| `POST /applications` | BORROWER | **Idempotency-Key required** |
| `GET /applications?status=A,B` | any | Borrowers only ever see their own |
| `GET /applications/{id}`, `GET /applications/{id}/schedule` | owner or staff | |
| `POST /applications/{id}/documents` | BORROWER | Multipart `docType` + `file`, PDF/JPEG/PNG up to 5 MiB, stored in GridFS |
| `GET /documents/{id}/content` | owner or staff | Authenticated file download; never a public Atlas URL |
| `POST /applications/{id}/info-response` | BORROWER | |
| `POST /applications/{id}/offer/accept` | BORROWER | **Idempotency-Key**; 410 after `offerExpiresAt` |
| `POST /applications/{id}/repayments` | BORROWER | **Idempotency-Key** |
| `POST /applications/{id}/assign` | LOAN_OFFICER | body carries `version` |
| `POST /applications/{id}/status` | officer, analyst, manager | body carries `version` |
| `PATCH /documents/{id}/verification` | analyst, officer | body carries `version` |
| `POST /applications/{id}/credit-check` | CREDIT_ANALYST | body carries `version` |
| `POST /applications/{id}/decision` | BRANCH_MANAGER | body carries `version` |
| `POST /applications/{id}/disbursement` | BRANCH_MANAGER | `version` and **Idempotency-Key** |
| `GET /staff` | staff | Active staff directory |
| `POST /staff` | ADMIN | Create staff; optional initial password, generated when omitted |
| `GET /staff/{id}` | ADMIN | Load an active staff account for editing |
| `PUT /staff/{id}` | ADMIN | Update fullName, email, phone, role and branch; keeps password |
| `DELETE /staff/{id}` | ADMIN | 204; revoke access and remove from the directory, retain history |
| `PUT /staff/{id}/credentials` | ADMIN | 204; set/replace only the password, using existing email as username |
| `GET /audit-logs` | ADMIN | |

Staff deletion sets the existing `users.active` flag to false. Loan assignments, decisions and audit
references remain intact, and the email stays reserved. Deleted accounts cannot sign in or use an
existing JWT. Each authenticated request uses the account's current role and active status from the
database, so changing a role also affects existing sessions on their next request.

Admins cannot delete their own account or remove their own admin role. Staff mutations lock the
active administrator rows and recheck the caller to prevent concurrent removals from leaving no
administrator. Updates and deletions create audit events. Bootstrap only creates an administrator
on an installation with no existing admin identities, so restarts do not recreate renamed/deleted
bootstrap accounts. Staff detail editing and deletion use the existing schema. No new environment variables are required.

### Staff credentials

The login username is the staff account's existing email. In the web staff list, the key icon opens
**Staff Credentials**, where email is read-only and only the password can be changed. The admin
enters and confirms a new password; changing their own password signs them out to sign in again.

Send an administrator Bearer token with `PUT /api/v1/staff/{id}/credentials` and JSON:

```json
{ "password": "NewPassword123" }
```

Success is HTTP 204 with no response body. Passwords require 8–72 characters, uppercase and lowercase
letters, and a number, with a maximum of 72 UTF-8 bytes for BCrypt. Password changes preserve email,
role, branch and other staff details. Only hashes are stored; audit events contain no password or hash.

`POST /api/v1/staff` also accepts an optional `password` alongside the normal staff details. Omit it
to retain automatic generation and the one-time `temporaryPassword` response. A supplied initial
password is never echoed; `temporaryPassword` is null. The web Add Staff form supports both choices.

Flyway migration `V4__credential_version.sql` adds a default-zero credential version. New JWTs carry
that version; password changes increment it, rejecting previous sessions on their next request.
Pre-migration tokens remain valid at version zero until a password change. The existing self-service
`PUT /api/v1/me/password` also revokes older sessions. Deploy the backend so Flyway applies V4 before
using the new web controls. No extra host environment variables are needed.

### Error format

Every error is the same JSON shape:

```json
{ "timestamp": "...", "status": 409, "code": "VERSION_CONFLICT",
  "message": "This record was changed by someone else. Reload it and try again.",
  "path": "/api/v1/applications/.../decision", "currentVersion": 4 }
```

Codes the clients should handle: `VERSION_CONFLICT` (409, reload and retry),
`DEADLINE_EXPIRED` (410), `RATE_LIMITED` (429), `SERVER_BUSY` (503, retry with the same
Idempotency-Key), `VALIDATION_FAILED` (400, see `fieldErrors`), `UNAUTHENTICATED` (401).

## Project layout

```
src/main/java/com/trustbank/loanapp/
  config/        thread pools, security chain, typed properties, demo seeder
  security/      JWT filter and service, rate/payload guard
  domain/        JPA entities and the LoanStatus state table
  repo/          Spring Data repositories
  idempotency/   at-most-once execution (Idempotency-Key)
  service/       business rules (stateless, local variables only)
  event/         pub/sub: events and their subscribers
  web/           REST controllers and DTOs
src/main/resources/
  application.yml
  db/migration/  Flyway SQL (V1 schema, V2 products)
```

See [GRIDFS-SETUP.md](GRIDFS-SETUP.md) for Atlas variables, upload examples, the admin workflow and host setup.
