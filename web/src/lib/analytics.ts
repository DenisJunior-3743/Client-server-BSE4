import type { LoanApplication, LoanStatus } from '@/types'
import { STATUS_META } from './status'

const APPROVED_LIKE: LoanStatus[] = ['APPROVED', 'PENDING_DISBURSEMENT', 'DISBURSED', 'ACTIVE', 'DELINQUENT', 'CLOSED', 'DEFAULTED']
const REJECTED_LIKE: LoanStatus[] = ['REJECTED', 'DOCUMENTS_REJECTED', 'CREDIT_CHECK_FAILED']
const PENDING_LIKE: LoanStatus[] = ['DRAFT', 'SUBMITTED', 'PENDING_DOCUMENTS', 'CREDIT_CHECK_PENDING', 'CREDIT_CHECK_PASSED', 'UNDER_REVIEW', 'AWAITING_ADDITIONAL_INFO']
const DISBURSED_LIKE: LoanStatus[] = ['DISBURSED', 'ACTIVE', 'DELINQUENT', 'CLOSED', 'DEFAULTED']

export interface Kpis {
  total: number
  approved: number
  rejected: number
  pending: number
  approvalRate: number
  totalDisbursedUgx: number
  avgAmountUgx: number
}

export function computeKpis(apps: LoanApplication[]): Kpis {
  const total = apps.length
  const approved = apps.filter((a) => APPROVED_LIKE.includes(a.status)).length
  const rejected = apps.filter((a) => REJECTED_LIKE.includes(a.status)).length
  const pending = apps.filter((a) => PENDING_LIKE.includes(a.status)).length
  const decided = approved + rejected
  const approvalRate = decided === 0 ? 0 : Math.round((approved / decided) * 100)
  const totalDisbursedUgx = apps
    .filter((a) => DISBURSED_LIKE.includes(a.status))
    .reduce((sum, a) => sum + (a.amountApproved ?? 0), 0)
  const avgAmountUgx = total === 0 ? 0 : Math.round(apps.reduce((sum, a) => sum + a.amountRequested, 0) / total)
  return { total, approved, rejected, pending, approvalRate, totalDisbursedUgx, avgAmountUgx }
}

export interface StatusCount {
  status: LoanStatus
  label: string
  tone: (typeof STATUS_META)[LoanStatus]['tone']
  count: number
}

export function statusBreakdown(apps: LoanApplication[]): StatusCount[] {
  const counts = new Map<LoanStatus, number>()
  for (const a of apps) counts.set(a.status, (counts.get(a.status) ?? 0) + 1)
  return Array.from(counts.entries())
    .map(([status, count]) => ({ status, count, ...STATUS_META[status] }))
    .sort((a, b) => b.count - a.count)
}

export interface MonthlyBucket {
  key: string
  label: string
  submitted: number
  disbursedUgx: number
}

export function monthlyTrend(apps: LoanApplication[], months = 6): MonthlyBucket[] {
  const now = new Date()
  const buckets: MonthlyBucket[] = []
  for (let i = months - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    buckets.push({
      key: `${d.getFullYear()}-${d.getMonth()}`,
      label: d.toLocaleString('en-GB', { month: 'short' }),
      submitted: 0,
      disbursedUgx: 0,
    })
  }
  const byKey = new Map(buckets.map((b) => [b.key, b]))
  for (const a of apps) {
    const submitted = new Date(a.submittedAt)
    const key = `${submitted.getFullYear()}-${submitted.getMonth()}`
    const bucket = byKey.get(key)
    if (!bucket) continue
    bucket.submitted += 1
    if (DISBURSED_LIKE.includes(a.status)) bucket.disbursedUgx += a.amountApproved ?? 0
  }
  return buckets
}
