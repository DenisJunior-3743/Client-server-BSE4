# Deploy the Spring Boot backend with Neon on Vercel

The complete monorepo is on `main`. The `backend` branch contains only `backend/` and basic repo support files. Deploy from either branch with Root Directory `backend`. The `mobile` branch now contains only Kotlin code.

## Local credentials

Real credentials are in ignored `backend/.env`. `backend/.env.example` contains placeholders and is safe to commit. The local helper loads `.env` without evaluating its contents, then restores the process environment when it exits:

```powershell
# From the repository root
.\backend\scripts\run-local.ps1
.\backend\scripts\run-local.ps1 -Action Test
.\backend\scripts\run-local.ps1 -Action Package
```

The app uses a direct Neon hostname, a JDBC URL, separate database username/password and a generated JWT signing secret. The Vercel/Neon integration exports PostgreSQL URLs in a different format; do not paste its raw `postgresql://user:password@...` value into Spring's `DATABASE_URL`. Use the normalized values in your private `.env` instead. JDBC uses `channelBinding`, whereas the original URI can use `channel_binding`. [Neon Java connection guide](https://neon.com/docs/guides/java).

## Import the backend

Vercel's Container Runtime is in beta and builds OCI images from a Dockerfile. This repo supplies `backend/Dockerfile` and `backend/vercel.json` for that path. [Container Runtime documentation](https://vercel.com/docs/functions/runtimes/container).

1. In Vercel create a project from `DenisJunior-3743/Client-server-BSE4`.
2. Select the `backend` branch for a backend-only deployment, or `main` for the complete monorepo. Set the production branch accordingly in Git settings.
3. Set **Root Directory** to `backend`.
4. Select the **Container** framework preset. Keep build and output overrides unset so the Dockerfile controls the build.
5. Add the variables below in Vercel's project environment settings before deployment. Use a dedicated preview Neon branch for preview deployments so they do not share production writes.

Vercel's file/project settings control the preset and build behavior. [Project configuration reference](https://vercel.com/docs/project-configuration).

| Variable | Value |
|---|---|
| `DATABASE_URL` | Private `.env` JDBC URL using the direct Neon host |
| `DATABASE_USER` | Private `.env` database username |
| `DATABASE_PASSWORD` | Private `.env` database password |
| `JWT_SECRET` | A strong persistent secret; your private `.env` has a generated one |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `CORS_ALLOWED_ORIGINS` | `https://client-server-bse-4.vercel.app` |
| `PORT` | `8080` |

`CORS_ALLOWED_ORIGINS` overrides the application default list. If already set on your backend host, add `https://client-server-bse-4.vercel.app` to its comma-separated values and redeploy the backend. Use origins without a trailing slash.

The Neon integration may already have provided a variable named `DATABASE_URL`. Override that value **in this backend project** with the JDBC version, and add the separately named user/password variables. Do not change another project's connection variables.

Do not prefix backend secrets with `VITE_` or put them in the React project. Production disables demo account seeding and OTP echo; it does not delete development accounts already present in a database. Before treating this database as production, replace/deactivate the known demo accounts through deliberate account administration. SMS delivery and payment settlement are still stubs, so this remains a coursework/demo deployment.

## Verify after deployment

Check these paths on the assigned backend HTTPS domain:

- `/actuator/health`
- `/swagger-ui.html`
- `/v3/api-docs`

Flyway uses a separate connection with longer migration timeouts; ordinary request connections keep their short lock/statement limits. Flyway applies pending migrations when the app starts. It should validate already-applied versions rather than recreate the database. Database errors appear in Vercel runtime logs. Never paste logs containing connection credentials into GitHub.

For a cross-platform development workflow check, run `python backend/scripts/smoke-test.py` against your local dev API. It creates test records and checks actual concurrent submissions and approvals.

For the React project use root `web`, Vite build `npm run build`, output `dist`, and `VITE_API_URL=https://<backend-domain>`. Redeploy after changing that build-time variable. For Android build from `mobile/` with `-PapiBaseUrl=https://<backend-domain>/`.

## Runtime limitation

Container Functions scale down when idle. Spring `@Scheduled` jobs only execute while the Java process is alive, so the daily delinquency job and cleanup jobs cannot be assumed to run on schedule on this hosting model. OTP/offer deadline checks still happen on the server when requests arrive. The current asynchronous event subscribers are also in-process and not durable. Production HTTP deadline checks remain independent of the sweeper jobs. [Container scale-in behavior](https://vercel.com/docs/functions/runtimes/container).

For reliable scheduled production work, use an always-on Java host or explicitly redesign jobs around an external scheduler/durable workers. This commit prepares deployment; it does not provision or claim to verify a live Vercel project.

Local validation builds and runs the executable jar. The Docker image has not been built locally because the Docker daemon is not running; Vercel will execute the Dockerfile during deployment.

## GridFS and initial admin

Configure the additional backend variables and Atlas network access described in [the GridFS setup guide](../backend/GRIDFS-SETUP.md). Neon remains required. The initial admin creates staff; sample staff seeding is disabled by default.

## API-only clients and staff navigation (2026-11-14)

Web and Android no longer contain client mock-data modes or fake sign-in. Configure the web build with `VITE_API_URL` and Android with `-PapiBaseUrl=https://<backend-domain>/` (or build environment `API_BASE_URL`). Admins see every web workspace; other staff see only their role's workspace, and direct module URLs are guarded by the same policy. Mobile CI is configured in the repository-root `.github/workflows/verify.yml`, with `mobile/` as the Android job working directory.
