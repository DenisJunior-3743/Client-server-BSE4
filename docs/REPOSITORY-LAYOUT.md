# Repository folders and branches

## The source layout

`main` contains `web/` (React), `mobile/` (Kotlin/Android), `backend/` (Spring Boot), and shared `docs/` and `diagrams/`. Each application owns its build files, dependencies and application README. There is one Git repository, not nested repositories.

The branches `web`, `mobile`, and `backend` contain only their corresponding project folder plus small root support files. For example the backend branch contains `backend/pom.xml`, not a root `pom.xml`. These branches are focused snapshots of the monorepo. Existing histories remain reachable; no force-push is required.

## Where to work

For ordinary learning and combined development, stay on `main` and open the folder you want in your editor. In Android Studio open `mobile/`. In Vercel the backend Root Directory stays `backend` and the React Root Directory stays `web`, whichever applicable branch you select.

Component branches deliberately omit other components. Git does not remove ignored local files when switching branches, so a leftover `node_modules`, `.env`, SDK setting or build cache may still be visible on disk. Those are local artifacts rather than additional committed components. Never delete the private database environment or signing key just to make a branch look empty.

## Safely bring a component change back to main

A folder-only branch has a different complete tree from `main`. Avoid merging its whole tree into `main`: that can remove unrelated project folders. Transfer only the component folder instead. Start with a clean working tree and commit your component work first.

Example for backend work:

```powershell
# After committing the backend changes on branch backend:
git switch main
git restore --source backend --staged --worktree -- backend
git diff --cached --stat
# Inspect and test, then commit and push main.
```

To refresh the backend snapshot after a backend change on main:

```powershell
git switch backend
git restore --source main --staged --worktree -- backend
git diff --cached --stat
# Inspect and test, then commit and push backend.
```

Use `web` or `mobile` in place of `backend` for those components. This transfers the complete named folder, including files removed within it; it leaves the other folders out of the operation. Avoid making unrelated changes during these commands. Shared documentation stays on `main`; each component branch has its own small CI workflow.

## Preserved local configuration

- Neon credentials: `backend/.env` (ignored).
- Android SDK location: `mobile/local.properties` (ignored).
- Android release keys and properties: `mobile/keystore/` (ignored).
- Package identity, version, API integration, styling and signing key remain the same.

The previous mixed layout is retained in Git history and in the reorganization's parent commits. The old `main` diagrams and reference documents have been restored to the complete monorepo rather than discarded.

Older coursework plans and diagrams are preserved as references. Use `docs/TASKS-COVERAGE.md` and `PROGRESS.md` for the current implementation and verified behavior.
