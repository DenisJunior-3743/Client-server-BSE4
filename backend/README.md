# TrustBank Loans: Backend API

Spring Boot REST API for the Bank Loan Application System (Client Server Programming, BSE4).
It serves the Kotlin mobile app (borrowers) and the React dashboard (bank staff), and stores
everything in PostgreSQL on Neon.

Progress, the course-concept mapping and next steps are in [PROGRESS.md](PROGRESS.md).

## Stack

| Layer | Technology |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5 (Web, Security, Data JPA, Validation, Actuator) |
| Database | PostgreSQL 16+ (Neon), schema owned by Flyway |
| Auth | JWT (HS256, jjwt), BCrypt password hashes |
| API description | OpenAPI via springdoc (`/swagger-ui.html`) |
| Build | Maven |

## Run it locally

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
   Flyway creates the tables on first start. The default `dev` profile also creates the seven
   staff accounts from the web repo's `DEMO_CREDENTIALS.md` (password `Passw0rd1`) and returns
   OTP codes in the API reply so you can test without an SMS gateway.

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
| `POST /applications/{id}/documents` | BORROWER | Metadata only for now |
| `POST /applications/{id}/info-response` | BORROWER | |
| `POST /applications/{id}/offer/accept` | BORROWER | **Idempotency-Key**; 410 after `offerExpiresAt` |
| `POST /applications/{id}/repayments` | BORROWER | **Idempotency-Key** |
| `POST /applications/{id}/assign` | LOAN_OFFICER | body carries `version` |
| `POST /applications/{id}/status` | officer, analyst, manager | body carries `version` |
| `PATCH /documents/{id}/verification` | analyst, officer | body carries `version` |
| `POST /applications/{id}/credit-check` | CREDIT_ANALYST | body carries `version` |
| `POST /applications/{id}/decision` | BRANCH_MANAGER | body carries `version` |
| `POST /applications/{id}/disbursement` | BRANCH_MANAGER | `version` and **Idempotency-Key** |
| `GET /staff` | staff | `POST /staff` is ADMIN only |
| `GET /audit-logs` | ADMIN | |

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
