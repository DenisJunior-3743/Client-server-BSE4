import type { LoanStatus } from '@/types'
import { STATUS_META } from '@/lib/status'
import { Badge } from './Badge'

export function StatusBadge({ status }: { status: LoanStatus }) {
  const meta = STATUS_META[status]
  return <Badge tone={meta.tone}>{meta.label}</Badge>
}
