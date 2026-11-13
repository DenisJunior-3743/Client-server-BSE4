# Run and deploy the backend

This component lives in `backend/` on both `main` and `backend` branches. The component branch contains no React or Kotlin source.

## Local run

Use Java 21. Inside `backend/`, put real local values in ignored `.env` using `.env.example` as a reference, then run:

```powershell
.\scripts\run-local.ps1
```

Use `-Action Test` or `-Action Package` for those operations. Direct `.\mvnw.cmd test` needs no database; `.\mvnw.cmd spring-boot:run` requires exported connection variables. The helper loads `.env` without evaluating its contents. Keep the JDBC URL, username/password and JWT secret private.

## Vercel

Import branch `backend` (or `main`), set Root Directory to `backend`, and use the Container preset. Add the private environment values in Vercel settings, set `SPRING_PROFILES_ACTIVE=prod`, `PORT=8080`, and set `CORS_ALLOWED_ORIGINS` to the React dashboard's exact HTTPS origin. The Dockerfile builds Java 21; `vercel.json` selects the Container preset.

Check `/actuator/health`, `/swagger-ui.html` and `/v3/api-docs` after deployment. Container instances scale down when idle, so reliable scheduled jobs need a separate scheduling/worker strategy. The SMS, credit bureau and payment settlement integrations remain stubs.

See the [full deployment guide on main](https://github.com/DenisJunior-3743/Client-server-BSE4/blob/main/docs/VERCEL-DEPLOYMENT.md) for details and current limitations.
