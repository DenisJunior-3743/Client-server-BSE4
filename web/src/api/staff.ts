import { staffUsers } from '@/mocks/data'
import { AVATAR_PALETTE } from '@/lib/avatarPalette'
import type { StaffRole, User } from '@/types'
import { delay } from './client'

export async function listStaff(): Promise<User[]> {
  await delay(300)
  return [...staffUsers]
}

export interface NewStaffInput {
  fullName: string
  email: string
  phone: string
  role: StaffRole
  branch: string
}

export async function createStaff(input: NewStaffInput): Promise<User> {
  await delay(400)
  const user: User = {
    id: `staff-${String(staffUsers.length + 1).padStart(2, '0')}`,
    email: input.email,
    fullName: input.fullName,
    phone: input.phone,
    role: input.role,
    branch: input.branch,
    avatarColor: AVATAR_PALETTE[staffUsers.length % AVATAR_PALETTE.length],
    createdAt: new Date().toISOString(),
  }
  staffUsers.push(user)
  return user
}
