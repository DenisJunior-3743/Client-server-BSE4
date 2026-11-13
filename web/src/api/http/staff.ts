import type { StaffCreated, StaffRole, User } from '@/types'
import { request } from '../client'

export function listStaff(): Promise<User[]> {
  return request<User[]>('/api/v1/staff')
}

export interface NewStaffInput {
  fullName: string
  email: string
  phone: string
  role: StaffRole
  branch: string
}

export function createStaff(input: NewStaffInput): Promise<StaffCreated> {
  return request<StaffCreated>('/api/v1/staff', { method: 'POST', body: input })
}
