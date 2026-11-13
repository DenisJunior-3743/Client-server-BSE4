// HTTP client for the Spring Boot API (Phase 2).
//
// VITE_API_URL decides the mode:
//   - unset  -> the Task 1 in-memory mocks (src/api/mock) keep working, no server needed
//   - set    -> every function in src/api talks to the real backend (src/api/http)
// Pages import from '@/api/*' and do not know which one they got.

export const API_URL: string = ((import.meta.env.VITE_API_URL as string | undefined) ?? '').replace(/\/+$/, '')
export const USE_MOCKS: boolean = API_URL === ''

const TOKEN_KEY = 'bankloan.token'
const USER_KEY = 'bankloan.currentUser'
const REQUEST_TIMEOUT_MS = 15_000

// Simulated network latency so loading states are visible in mock mode.
export function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/** Mirrors the backend's ApiError JSON. `code` is stable; `message` is safe to show to the user. */
export class ApiError extends Error {
  status: number
  code: string
  currentVersion?: number
  fieldErrors?: Record<string, string>

  constructor(status: number, code: string, message: string, extra?: { currentVersion?: number; fieldErrors?: Record<string, string> }) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.currentVersion = extra?.currentVersion
    this.fieldErrors = extra?.fieldErrors
  }
}

/** True when another officer changed the record first (HTTP 409 from the optimistic lock). */
export function isVersionConflict(err: unknown): err is ApiError {
  return err instanceof ApiError && err.code === 'VERSION_CONFLICT'
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/** One key per user action. Reuse the SAME key when retrying that action, never across actions. */
export function newIdempotencyKey(): string {
  return crypto.randomUUID()
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  /** Makes a POST safe to retransmit: the server runs it at most once and replays the first reply. */
  idempotencyKey?: string
  /** false for the public /auth endpoints */
  auth?: boolean
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET'
  const auth = options.auth ?? true
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (options.idempotencyKey) headers['Idempotency-Key'] = options.idempotencyKey
  const token = getToken()
  if (auth && token) headers.Authorization = `Bearer ${token}`

  // Only retransmit what is safe to repeat: reads, and writes that carry an Idempotency-Key.
  const retryable = method === 'GET' || options.idempotencyKey !== undefined
  const maxAttempts = retryable ? 3 : 1

  let lastError: ApiError = new ApiError(0, 'NETWORK', 'Cannot reach the server. Check your connection.')
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const controller = new AbortController()
    const timer = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)
    try {
      const response = await fetch(`${API_URL}${path}`, {
        method,
        headers,
        body: options.body === undefined ? undefined : JSON.stringify(options.body),
        signal: controller.signal,
      })
      if (response.ok) {
        return response.status === 204 ? (undefined as T) : ((await response.json()) as T)
      }
      const error = await toApiError(response)
      if (response.status === 401 && auth) {
        clearSession()
        window.location.assign('/login')
      }
      if (response.status !== 503) throw error
      lastError = error // server busy: retry if this request is retryable
    } catch (err) {
      if (err instanceof ApiError) throw err
      // fetch rejected: offline, DNS, timeout. The request may or may not have reached the server,
      // which is exactly why only idempotent requests are retried.
    } finally {
      window.clearTimeout(timer)
    }
    if (attempt < maxAttempts) await delay(400 * attempt)
  }
  throw lastError
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as {
      code?: string
      message?: string
      currentVersion?: number
      fieldErrors?: Record<string, string>
    }
    return new ApiError(response.status, body.code ?? 'ERROR', body.message ?? 'Request failed', {
      currentVersion: body.currentVersion,
      fieldErrors: body.fieldErrors,
    })
  } catch {
    return new ApiError(response.status, 'ERROR', `Request failed (${response.status})`)
  }
}

/** Downloads private documents using the same Bearer token as JSON API requests. */
export async function downloadDocument(documentId: string, fileName: string): Promise<void> {
  const token = getToken()
  const response = await fetch(`${API_URL}/api/v1/documents/${encodeURIComponent(documentId)}/content`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    signal: AbortSignal.timeout(30_000),
  })
  if (!response.ok) throw await toApiError(response)
  const blobUrl = URL.createObjectURL(await response.blob())
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000)
}
