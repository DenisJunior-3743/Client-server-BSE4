import { staffUsers, DEMO_PASSWORD } from '@/mocks/data'
import type { User } from '@/types'
import { delay } from './client'

export async function login(email: string, password: string): Promise<User> {
  await delay(500)
  const user = staffUsers.find((u) => u.email.toLowerCase() === email.trim().toLowerCase())
  if (!user || password !== DEMO_PASSWORD) {
    throw new Error('Invalid email or password')
  }
  return user
}
