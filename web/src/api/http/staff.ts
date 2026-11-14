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

export function getStaff(id: string): Promise<User> {
  return request<User>(`/api/v1/staff/${encodeURIComponent(id)}`)
}

export function updateStaff(id: string, input: NewStaffInput): Promise<User> {
  return request<User>(`/api/v1/staff/${encodeURIComponent(id)}`, { method: 'PUT', body: input })
}

export function deleteStaff(id: string): Promise<void> {
  return request<void>(`/api/v1/staff/${encodeURIComponent(id)}`, { method: 'DELETE' })
}
