import { auditLog } from '@/mocks/data'
import type { AuditLogEntry } from '@/types'
import { delay } from './client'

export async function listAuditLog(): Promise<AuditLogEntry[]> {
  await delay(300)
  return [...auditLog]
}
