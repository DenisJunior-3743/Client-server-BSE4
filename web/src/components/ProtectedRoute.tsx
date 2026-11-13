import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { canAccessPath, homeForRole } from '@/lib/access'
import { useAuth } from '@/state/AuthContext'

export function ProtectedRoute() {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (!canAccessPath(user.role, location.pathname)) {
    return <Navigate to={homeForRole(user.role)} replace />
  }
  return <Outlet />
}
