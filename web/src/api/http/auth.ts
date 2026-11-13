import type { User } from '@/types'
import { request, setToken } from '../client'

interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export async function login(email: string, password: string): Promise<User> {
  const reply = await request<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: { email: email.trim(), password },
    auth: false,
  })
  if (reply.user.role === 'BORROWER') {
    throw new Error('This dashboard is for bank staff. Borrowers use the mobile app.')
  }
  setToken(reply.accessToken)
  return reply.user
}
