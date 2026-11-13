import type { User } from '@/types'
import { request } from './client'

export type AccountDetails = Pick<User, 'fullName' | 'email' | 'phone'>

/** Saves the signed-in staff member's own details. PUT is idempotent: saving twice changes nothing more. */
export async function updateMyAccount(details: AccountDetails): Promise<AccountDetails> {
  const user = await request<User>('/api/v1/me', { method: 'PUT', body: details })
  return { fullName: user.fullName, email: user.email, phone: user.phone }
}
