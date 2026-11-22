import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import type { User } from '@/types'
import { login as apiLogin } from '@/api/auth'
import { clearSession, currentSession, storeProfile } from '@/api/session'

interface AuthContextValue {
  user: User | null
  isLoading: boolean
  error: string | null
  login: (email: string, password: string, rememberMe: boolean) => Promise<void>
  logout: () => void
  updateProfile: (updates: Pick<User, 'fullName' | 'email' | 'phone'>) => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => currentSession()?.user ?? null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    function syncRememberedSession(event: StorageEvent) {
      if (event.storageArea === localStorage) setUser(currentSession()?.user ?? null)
    }
    window.addEventListener('storage', syncRememberedSession)
    return () => window.removeEventListener('storage', syncRememberedSession)
  }, [])

  async function login(email: string, password: string, rememberMe: boolean) {
    setIsLoading(true)
    setError(null)
    try {
      const loggedInUser = await apiLogin(email, password, rememberMe)
      setUser(loggedInUser)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unable to log in'
      setError(message)
      throw err
    } finally {
      setIsLoading(false)
    }
  }

  function logout() {
    setUser(null)
    // Stateless server: logging out is simply forgetting the token on this device.
    clearSession()
  }

  function updateProfile(updates: Pick<User, 'fullName' | 'email' | 'phone'>) {
    setUser((prev) => {
      if (!prev) return prev
      const next = { ...prev, ...updates }
      storeProfile(next)
      return next
    })
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, error, login, logout, updateProfile }}>{children}</AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
