# Backend

Placeholder for the Client-Server BSE4 backend API (Phase 2+).

Per the project plan (see the `docs` branch, `PROJECT-PLAN.md`):

- **Framework:** Java Spring Boot (modular monolith)
- **Database:** PostgreSQL
- **Auth:** Spring Security + JWT
- **Workflow:** Spring Statemachine for loan status transitions
- **Async/events:** RabbitMQ + WebSocket push for live status/dashboard updates

Task 1 (web + mobile) runs entirely on mock data behind a thin API abstraction, so this backend
is not yet required to build those clients. This branch is a placeholder until Phase 2 work starts.
