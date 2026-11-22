import type { User } from '@/types'
import { isStaffRole } from '@/lib/access'

const SESSION_KEY = 'bankloan.session'
const LEGACY_TOKEN_KEY = 'bankloan.token'
const LEGACY_USER_KEY = 'bankloan.currentUser'

interface Session {
  accessToken: string
  user: User
  expiresAt: number | null
}

function removeSession(storage: Storage) {
  storage.removeItem(SESSION_KEY)
  storage.removeItem(LEGACY_TOKEN_KEY)
  storage.removeItem(LEGACY_USER_KEY)
}

function readSession(storage: Storage): Session | null {
  try {
    const raw = storage.getItem(SESSION_KEY)
    const legacyToken = storage.getItem(LEGACY_TOKEN_KEY)
    const legacyUser = storage.getItem(LEGACY_USER_KEY)
    const session: Session | null = raw ? JSON.parse(raw) : legacyToken && legacyUser
      ? { accessToken: legacyToken, user: JSON.parse(legacyUser), expiresAt: null }
      : null

    if (session?.accessToken && isStaffRole(session.user?.role) &&
        (session.expiresAt === null || session.expiresAt > Date.now())) return session
  } catch {
    // A malformed local record cannot establish a session.
  }
  removeSession(storage)
  return null
}

export function currentSession(): Session | null {
  return readSession(sessionStorage) ?? readSession(localStorage)
}

export function getToken(): string | null {
  return currentSession()?.accessToken ?? null
}

export function saveSession(accessToken: string, user: User, expiresIn: number, rememberMe: boolean) {
  clearSession()
  const storage = rememberMe ? localStorage : sessionStorage
  const session: Session = { accessToken, user, expiresAt: Date.now() + expiresIn * 1000 }
  storage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function storeProfile(user: User) {
  const tabSession = readSession(sessionStorage)
  const session = tabSession ?? readSession(localStorage)
  if (session?.user.id === user.id) {
    const storage = tabSession ? sessionStorage : localStorage
    storage.setItem(SESSION_KEY, JSON.stringify({ ...session, user }))
  }
}

export function clearSession() {
  removeSession(sessionStorage)
  removeSession(localStorage)
}
