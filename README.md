# TrustBank Loans

A bank loan processing system with three separate projects:

| Folder | Project | Open or run here |
|---|---|---|
| `web/` | React + TypeScript staff dashboard | `cd web; npm run dev` |
| `mobile/` | Kotlin + Jetpack Compose borrower app | Open `mobile/` in Android Studio |
| `backend/` | Java Spring Boot API | `cd backend; .\scripts\run-local.ps1` |
| `docs/` | Learning guides and coursework references | Start with `SPRING-BOOT-GUIDE.md` |
| `diagrams/` | Architecture, database and workflow diagrams | Source and rendered diagrams |

## Branch layout

- `main`: complete monorepo with all three project folders and shared documentation.
- `web`: only the `web/` project, plus a small repository README and CI configuration.
- `mobile`: only the `mobile/` project, plus a small repository README and CI configuration.
- `backend`: only the `backend/` project, plus a small repository README and CI configuration.
- `docs`: existing reference-document branch, preserved.

Use `main` to learn how the components connect. Use a component branch to study that component in isolation. See [docs/REPOSITORY-LAYOUT.md](docs/REPOSITORY-LAYOUT.md) for the normal component pull-request workflow and safely refreshing folder-only branches.

## Build each component

```powershell
# Android (Java 17 or 21 and Android SDK 34)
cd mobile
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest

# React (from the repository root)
cd web
npm ci
npm run build
npm run lint

# Spring Boot (from the repository root; Java 21)
cd backend
.\mvnw.cmd test
```

Each `cd` example starts independently from the repository root. Build outputs, local dependencies, credentials and signing keys are ignored.

## Run against the API

The real database settings are local in ignored `backend/.env`. `.env.example` contains placeholders. The helper `backend/scripts/run-local.ps1` loads that file and starts the API; use `-Action Test` or `-Action Package` for those checks.

For React, set `VITE_API_URL=http://localhost:8080` in `web/.env`, then run `npm run dev` in `web/`.

For Android, run inside `mobile/`:

```powershell
.\gradlew.bat installDebug -PapiBaseUrl=http://10.0.2.2:8080/
```

`10.0.2.2` reaches your PC from an emulator. Use the PC's LAN address for a physical phone. Both clients use the backend exclusively; Android debug builds default to the local emulator URL. Configure the deployed API URL for hosting and physical devices.

## Learning and deployment

- [Spring Boot beginner guide](docs/SPRING-BOOT-GUIDE.md)
- [Android guide](mobile/README.md)
- [Coursework implementation map](docs/TASKS-COVERAGE.md)
- [Local setup and deployment](DEPLOYMENT.md)
- [Vercel backend deployment](docs/VERCEL-DEPLOYMENT.md)
- [Verified project status](PROGRESS.md)

For Vercel backend hosting, select branch `backend` (or `main`) and Root Directory `backend`. For React select branch `web` (or `main`) and Root Directory `web`.

## API-only clients and staff navigation (2026-11-14)

Web and Android no longer contain client mock-data modes or fake sign-in. Configure the web build with `VITE_API_URL` and Android with `-PapiBaseUrl=https://<backend-domain>/` (or build environment `API_BASE_URL`). Admins see every web workspace; other staff see only their role's workspace, and direct module URLs are guarded by the same policy. Mobile CI is configured in the repository-root `.github/workflows/verify.yml`, with `mobile/` as the Android job working directory.

## Mobile releases

[Build a signed APK and publish by tag](mobile/REVIDEN-RELEASE.md). The Reviden release workflow supports main and mobile branch commits, testing/production channels, existing-key verification, artifact retention and explicit store publication.
