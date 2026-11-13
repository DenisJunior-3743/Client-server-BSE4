# Backend status

Java 21 / Spring Boot API with PostgreSQL (Neon), Flyway migrations, JWT role checks, optimistic versions, duplicate-request protection and asynchronous events.

The last verification passed 18 unit tests and a full Neon API smoke test, including concurrent duplicate submissions and approvals, offer acceptance, payment replay, authorization, notifications and audit persistence. Swagger UI and OpenAPI were verified locally. Vercel container configuration is prepared; live hosting remains to configure.

Current system-wide status is maintained on the [main branch](https://github.com/DenisJunior-3743/Client-server-BSE4/blob/main/PROGRESS.md). See `DEPLOYMENT.md` in this component for local and hosted setup.
