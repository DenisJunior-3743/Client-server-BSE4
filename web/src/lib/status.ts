import type { LoanStatus } from '@/types'

export type StatusTone = 'neutral' | 'primary' | 'warning' | 'success' | 'danger'

export const STATUS_META: Record<LoanStatus, { label: string; tone: StatusTone }> = {
  DRAFT: { label: 'Draft', tone: 'neutral' },
  SUBMITTED: { label: 'Submitted', tone: 'primary' },
  PENDING_DOCUMENTS: { label: 'Pending Documents', tone: 'warning' },
  DOCUMENTS_REJECTED: { label: 'Documents Rejected', tone: 'danger' },
  CREDIT_CHECK_PENDING: { label: 'Credit Check Pending', tone: 'primary' },
  CREDIT_CHECK_FAILED: { label: 'Credit Check Failed', tone: 'danger' },
  CREDIT_CHECK_PASSED: { label: 'Credit Check Passed', tone: 'success' },
  UNDER_REVIEW: { label: 'Under Review', tone: 'primary' },
  AWAITING_ADDITIONAL_INFO: { label: 'Awaiting Info', tone: 'warning' },
  APPROVED: { label: 'Approved', tone: 'success' },
  REJECTED: { label: 'Rejected', tone: 'danger' },
  PENDING_DISBURSEMENT: { label: 'Pending Disbursement', tone: 'primary' },
  DISBURSED: { label: 'Disbursed', tone: 'success' },
  ACTIVE: { label: 'Active', tone: 'success' },
  DELINQUENT: { label: 'Delinquent', tone: 'warning' },
  CLOSED: { label: 'Closed', tone: 'neutral' },
  DEFAULTED: { label: 'Defaulted', tone: 'danger' },
}

export const STAFF_ROLE_META: Record<string, { label: string }> = {
  LOAN_OFFICER: { label: 'Loan Officer' },
  CREDIT_ANALYST: { label: 'Credit Analyst' },
  BRANCH_MANAGER: { label: 'Branch Manager' },
  ADMIN: { label: 'Admin' },
  BORROWER: { label: 'Borrower' },
}
