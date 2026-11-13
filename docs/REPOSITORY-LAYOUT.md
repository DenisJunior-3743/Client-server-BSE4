# Repository folders and branches

## Layout

`main` contains `web/` (React), `mobile/` (Kotlin/Android), `backend/` (Spring Boot), and shared documentation/diagrams. Each application owns its build files and README. There is one Git repository.

The branches `web`, `mobile` and `backend` contain only their corresponding project folder plus basic repository support files. On GitHub, choose one of those branches to study a component alone. For combined development, use `main` and open the folder you want; open `mobile/` in Android Studio.

## Why the first merges needed repair

Creating a folder-only snapshot records deletions of every other folder. The first web merge brought those deletions into main. The monorepo has now been restored, and the initial backend/mobile snapshot commits are explicitly integrated while retaining all three components.

Those initial snapshot commits are now part of main's ancestry. Future commits on the existing component branches should edit only that component folder. Normal pull requests/merges then compare against the already-integrated snapshot rather than reapplying the original split's deletions.

Do not recreate a component branch from a new main commit and delete the other folders again. That would create a new set of deletions to reconcile.

## Normal component work and merging

Start with a clean working tree. Example for backend work:

```powershell
git switch backend
git pull --ff-only origin backend
# Edit files inside backend/, then commit and push your work.
```

Open a GitHub pull request with base `main` and compare `backend`. Check Files changed: it should contain your backend changes, not deletions of web/, mobile/ or shared docs. Shared README/CI changes should normally be made on main.

The equivalent local merge, after committing the component work, is:

```powershell
git switch main
git pull --ff-only origin main
git merge --no-ff backend
# Resolve any genuine same-file conflicts, run relevant checks, then:
git push origin main
```

Use `mobile` or `web` in place of `backend` for those components. If the current component tip is already merged, Git correctly reports that main is up to date.

## Bring a main-side component change into its focused branch

Keep the focused branches small: transfer only their folder from main, rather than merging the whole monorepo into a component branch. With a clean working tree:

```powershell
git fetch origin
git switch backend
git pull --ff-only origin backend
git restore --source origin/main --staged --worktree -- backend
git diff --cached --stat
# Inspect, test, commit and push backend.
```

This copies the named folder, including removals inside it. It does not add other project folders. Shared documentation remains on main; each component branch has its own CI workflow.

## Local configuration

- Neon credentials: `backend/.env` (ignored).
- Android SDK location: `mobile/local.properties` (ignored).
- Android signing material: `mobile/keystore/` (ignored).

Ignored caches and private files may remain visible on disk when switching branches; they are not extra committed components. Do not delete them just to make a checkout look empty.

Existing histories, coursework documents and diagrams are preserved. Use `docs/TASKS-COVERAGE.md` and `PROGRESS.md` for the current implementation and verification results.
