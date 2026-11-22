import { LoadError } from '@/components/ui/LoadError'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Inbox } from 'lucide-react'
import type { LoanApplication } from '@/types'
import { listApplications } from '@/api/applications'
import { useProducts } from '@/lib/useProducts'
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
  const [loadError, setLoadError] = useState<string | null>(null)
  const navigate = useNavigate()
  const loanProducts = useProducts()

  useEffect(() => {
    let active = true
    listApplications()
      .then((data) => {
        if (active) setApplications(filter(data))
      })
      .catch((error: unknown) => {
        if (active) setLoadError(error instanceof Error ? error.message : 'Unable to load applications')
      })
    return () => {
      active = false
    }
  }, [filter])

  if (loadError) return <LoadError message={loadError} />
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
          <table aria-label={title} className="block w-full text-left text-sm md:table md:min-w-[720px]">
            <thead className="sr-only md:not-sr-only">
              <tr className="border-b border-neutral-200 bg-neutral-50 text-xs uppercase tracking-wide text-neutral-500">
                <th className="px-5 py-3 font-semibold">Applicant</th>
                <th className="px-5 py-3 font-semibold">Product</th>
                <th className="px-5 py-3 font-semibold">Amount</th>
                <th className="px-5 py-3 font-semibold">Submitted</th>
                <th className="px-5 py-3 font-semibold">Status</th>
              </tr>
            </thead>
            <tbody className="block md:table-row-group">
              {applications.map((app) => {
                const product = loanProducts.find((p) => p.id === app.productId)
                return (
                  <tr
                    key={app.id}
                    onClick={() => navigate(`/applications/${app.id}`)}
                    className="grid cursor-pointer grid-cols-2 gap-x-3 gap-y-3 border-b border-neutral-100 px-3 py-4 transition-colors last:border-0 hover:bg-primary-50/40 md:table-row md:p-0"
                  >
                    <td className="col-span-2 min-w-0 md:px-5 md:py-3.5">
                      <div className="flex min-w-0 items-center gap-3">
                        <Avatar name={app.applicantName} color={colorForId(app.applicantId)} size={32} />
                        <div className="min-w-0">
                          <Link to={`/applications/${app.id}`} onClick={(event) => event.stopPropagation()}
                            className="font-medium text-neutral-900 wrap-anywhere hover:underline">{app.applicantName}</Link>
                          <p className="text-xs text-neutral-500 wrap-anywhere">
                            {app.applicantPhone} · {app.id}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="text-neutral-600 wrap-anywhere md:px-5 md:py-3.5">
                      <span className="block text-xs text-neutral-500 md:hidden">Product</span>
                      {product?.name ?? app.productType}
                    </td>
                    <td className="font-medium text-neutral-900 wrap-anywhere md:px-5 md:py-3.5">
                      <span className="block text-xs font-normal text-neutral-500 md:hidden">Amount</span>
                      {formatUGX(app.amountRequested)}
                    </td>
                    <td className="text-neutral-600 md:px-5 md:py-3.5">
                      <span className="block text-xs text-neutral-500 md:hidden">Submitted</span>
                      {formatDate(app.submittedAt)}
                    </td>
                    <td className="self-center md:px-5 md:py-3.5">
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
