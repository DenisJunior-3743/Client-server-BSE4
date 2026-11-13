# Atlas documents and the initial admin

Neon still stores users, staff, loans and document metadata. MongoDB Atlas stores the actual PDF/JPEG/PNG bytes in GridFS. The mobile app sends files to Spring Boot using its JWT; Atlas credentials belong only on the backend.

## Backend hosting variables

Add these to the environment of the **backend** project, for the environments you deploy. Redeploy after setting them.

| Variable | Value |
|---|---|
| `MONGODB_URI` | The resolved private URI from ignored `backend/.env`, with actual database credentials |
| `MONGODB_DATABASE` | `trustbank_documents` |
| `MONGODB_GRIDFS_BUCKET` | `loan_documents` |
| `BOOTSTRAP_ADMIN_ENABLED` | `true` for initial provisioning; can be `false` afterwards |
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@bank.com` |
| `BOOTSTRAP_ADMIN_PASSWORD` | `Admin123` for the requested initial account, or your own password before its first creation |
| `SEED_DEMO_DATA` | `false` |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `CORS_ALLOWED_ORIGINS` | `https://client-server-bse-4.vercel.app` (plus any other required origins, comma separated) |

Keep the existing `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `JWT_SECRET` and `PORT` values. These are additional settings, not replacements for Neon. MongoDB database and bucket names have the defaults shown above. `MONGODB_USERNAME` and `MONGODB_PASSWORD` are not separate runtime settings: they are already encoded in `MONGODB_URI`. Never use `VITE_` for these variables or embed the URI in the Android app.

The local `atlas-credentials.env` file is ignored by Git. Its URI and credentials have been normalized into ignored `backend/.env`, which the local run helper loads. Hosting does not read local `.env` files: copy the values into the backend host's environment settings.

In Atlas, use a **database user** with `readWrite` access to `trustbank_documents` and allow the backend's outbound IP addresses in the project's Network Access list. The Atlas website login is not a database user. Password/user characters in a URI must be percent encoded. [Atlas connection prerequisites](https://www.mongodb.com/docs/atlas/connect-to-database-deployment/).

GridFS creates `loan_documents.files` and `loan_documents.chunks` on the first upload; no manual collection creation is needed. [MongoDB Java GridFS guide](https://www.mongodb.com/docs/drivers/java/sync/current/crud/gridfs/).

## Sign in and register staff

1. Start/redeploy the backend. Flyway adds the GridFS reference column without deleting old records.
2. Sign into the web dashboard using `admin@bank.com` / `Admin123` (unless you selected another bootstrap password before first startup).
3. Change the initial password in account settings (`PUT /api/v1/me/password`). Bootstrap never overwrites an existing password.
4. Open staff management and create officers, credit analysts or branch managers. `POST /api/v1/staff` is restricted to the `ADMIN` role and returns the new user's temporary password once.

Existing users, including old demo staff, are preserved. Disabling demo seeding does not delete them. If the bootstrap email already belongs to a borrower, startup fails instead of promoting that account.

## Mobile and API flow

Build the Android app with `-PapiBaseUrl=https://<your-backend-domain>/`. The document step opens the Android system file picker. Select National ID, payslip and bank statement, then submit. Files upload individually after the loan is created. If an upload fails, retry finishes the remaining uploads against the same loan. Documents can also be added/replaced on the application detail screen in `SUBMITTED`, `PENDING_DOCUMENTS` or `AWAITING_ADDITIONAL_INFO`.

- `POST /api/v1/applications/{id}/documents`: `multipart/form-data`, text part `docType` and binary part `file`. Borrowers can upload only to their own open applications.
- `GET /api/v1/documents/{id}/content`: requires a Bearer token from the owner or staff. Responses use attachment disposition and `Cache-Control: no-store`.
- The dashboard's **Download** button sends the staff JWT; staff can inspect the file before verifying/rejecting it.
- PDFs, JPEGs and PNGs must match their file signature, be nonempty and be at most 5 MiB. The multipart request limit is 6 MiB. The server generates the GridFS reference; clients cannot register fake metadata as an upload.
- Document responses include `downloadUrl`, `contentType` and `sizeBytes`. Old metadata-only rows have no download URL and need a real upload.
- Credit checks require the three mandatory types and verification of the latest real upload of each type. A replacement supersedes older rejected/pending files.
- SQL rollback triggers best-effort deletion of the corresponding GridFS upload. The databases do not share a transaction; a process crash between stores can leave an orphan that needs reconciliation.

Swagger documents these endpoints at `https://<your-backend-domain>/swagger-ui.html`; JSON schema is at `/v3/api-docs`. Sign in through `/api/v1/auth/login`, then use Swagger's **Authorize** button with the access token.

Example (replace placeholders; do not save a real token in Git):

```sh
curl -X POST 'https://<backend-domain>/api/v1/applications/<loan-id>/documents' \
  -H 'Authorization: Bearer <borrower-token>' \
  -F 'docType=NATIONAL_ID' -F 'file=@national-id.pdf;type=application/pdf'
```

The hosting platform can impose a lower request limit than Spring. Vercel documents a 4.5 MB Function payload limit; keep files below 4 MiB there and check the limits for the runtime enabled on your project. [Vercel Function limits](https://vercel.com/docs/functions/limitations).

## Verification

Ordinary Maven tests mock storage and need no Atlas access. For a real temporary upload/read/delete check, load `backend/.env`, set `RUN_GRIDFS_INTEGRATION_TESTS=true` and run Maven tests. The optional test creates a non-customer connectivity-check file, compares the returned bytes and deletes it.

The API smoke test (`python backend/scripts/smoke-test.py`) creates development borrowers, staff, loans and uploaded files that remain in the database. It signs in with the bootstrap admin environment variables/defaults, creates its staff through the admin endpoint, and tests the complete loan workflow. Run it against a development database.
