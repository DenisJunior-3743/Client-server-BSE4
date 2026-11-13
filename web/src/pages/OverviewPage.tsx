import { useEffect, useState } from 'react'
import { FileStack, TrendingUp, Wallet, Clock4 } from 'lucide-react'
import type { LoanApplication, LoanStatus } from '@/types'
import { listApplications } from '@/api/applications'
import { computeKpis, statusBreakdown, monthlyTrend } from '@/lib/analytics'
import { formatUGX } from '@/lib/format'
import { TONE_HEX } from '@/lib/chartColors'
import { PageHeader } from '@/components/layout/PageHeader'
import { PageSpinner } from '@/components/ui/PageSpinner'
import { StatCard } from '@/components/ui/StatCard'
import { Card, CardHeader, CardTitle } from '@/components/ui/Card'
import { StatusBarChart } from '@/components/charts/StatusBarChart'
import { DonutChart } from '@/components/charts/DonutChart'
import { TrendAreaChart } from '@/components/charts/TrendAreaChart'

interface OverviewPageProps {
  title: string
  description: string
  scopeStatuses?: LoanStatus[]
}

export function OverviewPage({ title, description, scopeStatuses }: OverviewPageProps) {
  const [applications, setApplications] = useState<LoanApplication[] | null>(null)

  useEffect(() => {
    let active = true
    listApplications(scopeStatuses ? { statuses: scopeStatuses } : undefined).then((data) => {
      if (active) setApplications(data)
    })
    return () => {
      active = false
    }
  }, [scopeStatuses])

  if (!applications) return <PageSpinner />

  const kpis = computeKpis(applications)
  const breakdown = statusBreakdown(applications)
  const trend = monthlyTrend(applications).map((b) => ({ label: b.label, value: b.submitted }))
  const decisionSplit = [
    { label: 'Approved', value: kpis.approved, color: TONE_HEX.success },
    { label: 'Rejected', value: kpis.rejected, color: TONE_HEX.danger },
    { label: 'Pending', value: kpis.pending, color: TONE_HEX.primary },
  ].filter((d) => d.value > 0)

  return (
    <div>
      <PageHeader title={title} description={description} />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Applications in scope" value={String(kpis.total)} icon={FileStack} tone="primary" />
        <StatCard
          label="Approval rate"
          value={`${kpis.approvalRate}%`}
          icon={TrendingUp}
          tone="success"
          hint={`${kpis.approved} approved · ${kpis.rejected} rejected`}
        />
        <StatCard label="Total disbursed" value={formatUGX(kpis.totalDisbursedUgx)} icon={Wallet} tone="warning" />
        <StatCard label="Awaiting action" value={String(kpis.pending)} icon={Clock4} tone="danger" />
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader>
            <CardTitle>Applications by Status</CardTitle>
          </CardHeader>
          {breakdown.length === 0 ? (
            <p className="py-10 text-center text-sm text-neutral-500">No applications in this scope yet.</p>
          ) : (
            <StatusBarChart data={breakdown} />
          )}
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Decision Split</CardTitle>
          </CardHeader>
          {decisionSplit.length === 0 ? (
            <p className="py-10 text-center text-sm text-neutral-500">No decisions yet.</p>
          ) : (
            <>
              <DonutChart data={decisionSplit} />
              <ul className="mt-2 flex flex-col gap-2">
                {decisionSplit.map((d) => (
                  <li key={d.label} className="flex items-center justify-between text-sm">
                    <span className="flex items-center gap-2 text-neutral-600">
                      <span className="h-2.5 w-2.5 rounded-full" style={{ background: d.color }} />
                      {d.label}
                    </span>
                    <span className="font-semibold text-neutral-900">{d.value}</span>
                  </li>
                ))}
              </ul>
            </>
          )}
        </Card>
      </div>

      <Card className="mt-4">
        <CardHeader>
          <CardTitle>Applications Submitted — Last 6 Months</CardTitle>
        </CardHeader>
        <TrendAreaChart data={trend} color={TONE_HEX.primary} />
      </Card>
    </div>
  )
}
