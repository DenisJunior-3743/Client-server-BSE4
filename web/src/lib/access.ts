import type { StaffRole, UserRole } from '../types/index.ts'

export const WORKSPACE_ROLES = {
  officer: 'LOAN_OFFICER', analyst: 'CREDIT_ANALYST', manager: 'BRANCH_MANAGER', admin: 'ADMIN',
} as const satisfies Record<string, StaffRole>

export function isStaffRole(role: unknown): role is StaffRole {
  return typeof role === 'string' && Object.values(WORKSPACE_ROLES).some((value) => value === role)
}

export function canAccessWorkspace(role: UserRole | undefined, workspaceRole: StaffRole): boolean {
  return role === 'ADMIN' || role === workspaceRole
}

export function homeForRole(role: UserRole | undefined): string {
  if (role === 'ADMIN') return '/'
  const workspace = Object.entries(WORKSPACE_ROLES).find(([, value]) => value === role)?.[0]
  return workspace ? `/${workspace}/overview` : '/login'
}

export function canAccessPath(role: UserRole | undefined, pathname: string): boolean {
  if (!isStaffRole(role)) return false
  if (pathname === '/') return role === 'ADMIN'
  const segment = pathname.split('/')[1]
  const workspaceRole = WORKSPACE_ROLES[segment as keyof typeof WORKSPACE_ROLES]
  return workspaceRole ? canAccessWorkspace(role, workspaceRole) : true
}
