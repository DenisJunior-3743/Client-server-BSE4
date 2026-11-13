# Backend status

Java 21 / Spring Boot API with PostgreSQL (Neon), Flyway migrations, JWT role checks, optimistic versions, duplicate-request protection and asynchronous events.

The last verification passed 18 unit tests and a full Neon API smoke test, including concurrent duplicate submissions and approvals, offer acceptance, payment replay, authorization, notifications and audit persistence. Swagger UI and OpenAPI were verified locally. Vercel container configuration is prepared; live hosting remains to configure.

Current system-wide status is maintained on the [main branch](https://github.com/DenisJunior-3743/Client-server-BSE4/blob/main/PROGRESS.md). See `DEPLOYMENT.md` in this component for local and hosted setup.

## GridFS documents and initial admin (2026-11-13)

The backend accepts real multipart PDF/JPEG/PNG uploads, stores file bytes in Atlas GridFS, and keeps private GridFS references in Neon via Flyway V3. Owner/staff downloads require JWT authentication. The mobile app now selects and uploads files during application submission and from application detail; staff can download them from the web dashboard. Credit checks require real files and verification of the latest upload of each required type.

The initial admin defaults to `admin@bank.com` / `Admin123`, stores a BCrypt hash, and is created once without resetting existing passwords. Staff registration remains admin-only. Automatic sample staff seeding is disabled by default. Atlas secrets stay in ignored `atlas-credentials.env` and `backend/.env`. Deployment variables and setup are documented in `backend/GRIDFS-SETUP.md` (or `GRIDFS-SETUP.md` from the backend folder).

Validation: 33 backend unit tests passed; the opt-in Atlas upload/read/delete check passed separately. The packaged API passed health/OpenAPI checks and the full Neon/GridFS workflow smoke test, including admin-created staff, real document uploads/downloads, credit checks, concurrent approvals, disbursement and repayments. The web production build and lint passed. Live smoke-test development records remain in the databases.
