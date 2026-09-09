import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/state/AuthContext'

export function ProtectedRoute() {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}
