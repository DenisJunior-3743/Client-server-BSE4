import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Inbox } from 'lucide-react'
import type { LoanApplication } from '@/types'
import { listApplications } from '@/api/applications'
import { loanProducts } from '@/mocks/data'
import { formatUGX, formatDate } from '@/lib/format'
import { colorForId } from '@/lib/avatarPalette'
import { PageHeader } from '@/components/layout/PageHeader'
import { PageSpinner } from '@/components/ui/PageSpinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { Avatar } from '@/components/ui/Avatar'

interface ApplicationsQueuePageProps {
  title: string
  description: string
  filter: (apps: LoanApplication[]) => LoanApplication[]
  emptyMessage?: string
}

export function ApplicationsQueuePage({ title, description, filter, emptyMessage }: ApplicationsQueuePageProps) {
  const [applications, setApplications] = useState<LoanApplication[] | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    let active = true
    listApplications().then((data) => {
      if (active) setApplications(filter(data))
    })
    return () => {
      active = false
    }
  }, [filter])

  if (!applications) return <PageSpinner />

  return (
    <div>
      <PageHeader
        title={title}
        description={description}
        actions={
          <span className="text-sm font-medium text-neutral-500">
            {applications.length} application{applications.length === 1 ? '' : 's'}
          </span>
        }
      />

      {applications.length === 0 ? (
        <EmptyState
          icon={Inbox}
          title="Nothing here right now"
          description={emptyMessage ?? 'New items will show up here as applications move through the workflow.'}
        />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-neutral-200 bg-white shadow-[var(--shadow-card)]">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead>
              <tr className="border-b border-neutral-200 bg-neutral-50 text-xs uppercase tracking-wide text-neutral-500">
                <th className="px-5 py-3 font-semibold">Applicant</th>
                <th className="px-5 py-3 font-semibold">Product</th>
                <th className="px-5 py-3 font-semibold">Amount</th>
                <th className="px-5 py-3 font-semibold">Submitted</th>
                <th className="px-5 py-3 font-semibold">Status</th>
              </tr>
            </thead>
            <tbody>
              {applications.map((app) => {
                const product = loanProducts.find((p) => p.id === app.productId)
                return (
                  <tr
                    key={app.id}
                    onClick={() => navigate(`/applications/${app.id}`)}
                    className="cursor-pointer border-b border-neutral-100 transition-colors last:border-0 hover:bg-primary-50/40"
                  >
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        <Avatar name={app.applicantName} color={colorForId(app.applicantId)} size={32} />
                        <div>
                          <p className="font-medium text-neutral-900">{app.applicantName}</p>
                          <p className="text-xs text-neutral-500">
                            {app.applicantPhone} · {app.id}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-neutral-600">{product?.name ?? app.productType}</td>
                    <td className="px-5 py-3.5 font-medium text-neutral-900">{formatUGX(app.amountRequested)}</td>
                    <td className="px-5 py-3.5 text-neutral-600">{formatDate(app.submittedAt)}</td>
                    <td className="px-5 py-3.5">
                      <StatusBadge status={app.status} />
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
