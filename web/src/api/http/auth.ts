import type { User } from '@/types'
import { request } from '../client'
import { saveSession } from '../session'

interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export async function login(email: string, password: string, rememberMe: boolean): Promise<User> {
  const reply = await request<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: { email: email.trim(), password, rememberMe },
    auth: false,
  })
  if (reply.user.role === 'BORROWER') {
    throw new Error('This dashboard is for bank staff. Borrowers use the mobile app.')
  }
  saveSession(reply.accessToken, reply.user, reply.expiresIn, rememberMe)
  return reply.user
}
