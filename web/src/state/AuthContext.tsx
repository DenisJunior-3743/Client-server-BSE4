import { createContext, useContext, useState, type ReactNode } from 'react'
import type { User } from '@/types'
import { login as apiLogin } from '@/api/auth'

interface AuthContextValue {
  user: User | null
  isLoading: boolean
  error: string | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  updateProfile: (updates: Pick<User, 'fullName' | 'email' | 'phone'>) => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)
const STORAGE_KEY = 'bankloan.currentUser'

function readStoredUser(): User | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(readStoredUser)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function login(email: string, password: string) {
    setIsLoading(true)
    setError(null)
    try {
      const loggedInUser = await apiLogin(email, password)
      setUser(loggedInUser)
      localStorage.setItem(STORAGE_KEY, JSON.stringify(loggedInUser))
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
    localStorage.removeItem(STORAGE_KEY)
  }

  function updateProfile(updates: Pick<User, 'fullName' | 'email' | 'phone'>) {
    setUser((prev) => {
      if (!prev) return prev
      const next = { ...prev, ...updates }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
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
