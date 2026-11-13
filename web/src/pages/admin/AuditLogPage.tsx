import { useEffect, useState } from 'react'
import { ScrollText } from 'lucide-react'
import { listAuditLog } from '@/api/audit'
import type { AuditLogEntry } from '@/types'
import { formatDateTime } from '@/lib/format'
import { PageHeader } from '@/components/layout/PageHeader'
import { PageSpinner } from '@/components/ui/PageSpinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Badge } from '@/components/ui/Badge'

function toneForAction(action: string) {
  if (action.includes('REJECT') || action.includes('DEACTIVATE')) return 'danger' as const
  if (action.includes('APPROVE') || action.includes('CREATE')) return 'success' as const
  if (action.includes('UPDATE')) return 'warning' as const
  return 'neutral' as const
}

export default function AuditLogPage() {
  const [entries, setEntries] = useState<AuditLogEntry[] | null>(null)

  useEffect(() => {
    listAuditLog().then(setEntries)
  }, [])

  if (!entries) return <PageSpinner />

  return (
    <div>
      <PageHeader title="Audit Log" description="Who did what, when — across the whole system." />
      {entries.length === 0 ? (
        <EmptyState icon={ScrollText} title="No activity recorded yet" />
      ) : (
        <div className="overflow-hidden rounded-2xl border border-neutral-200 bg-white shadow-[var(--shadow-card)]">
          <ul className="divide-y divide-neutral-100">
            {entries.map((entry) => (
              <li key={entry.id} className="flex flex-wrap items-start justify-between gap-3 px-5 py-4">
                <div>
                  <div className="flex items-center gap-2">
                    <Badge tone={toneForAction(entry.action)}>{entry.action.replace(/_/g, ' ')}</Badge>
                    <span className="text-sm font-medium text-neutral-800">{entry.performedByName}</span>
                  </div>
                  <p className="mt-1.5 text-sm text-neutral-600">{entry.details}</p>
                </div>
                <p className="shrink-0 text-xs text-neutral-400">{formatDateTime(entry.performedAt)}</p>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
