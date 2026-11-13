import type { LoanApplication, LoanStatus } from '@/types'

// Queue filters use status and pending counts returned by the backend.
export const officerNewFilter = (apps: LoanApplication[]) => apps.filter((a) => a.status === 'SUBMITTED' && !a.assignedOfficerId)

export const officerInReviewFilter = (apps: LoanApplication[]) => apps.filter((a) => a.status === 'SUBMITTED' && !!a.assignedOfficerId)

export const officerInfoRequestsFilter = (apps: LoanApplication[]) => apps.filter((a) => a.status === 'AWAITING_ADDITIONAL_INFO')

export const analystVerificationFilter = (apps: LoanApplication[]) =>
  apps.filter((a) => (a.pendingDocuments ?? 0) > 0)

export const analystCreditAssessmentFilter = (apps: LoanApplication[]) => apps.filter((a) => a.status === 'CREDIT_CHECK_PENDING')

export const managerPendingApprovalFilter = (apps: LoanApplication[]) => apps.filter((a) => a.status === 'UNDER_REVIEW')

const APPROVED_QUEUE_STATUSES: LoanStatus[] = ['APPROVED', 'PENDING_DISBURSEMENT', 'DISBURSED', 'ACTIVE', 'CLOSED']
export const managerApprovedFilter = (apps: LoanApplication[]) => apps.filter((a) => APPROVED_QUEUE_STATUSES.includes(a.status))

const REJECTED_QUEUE_STATUSES: LoanStatus[] = ['REJECTED', 'DOCUMENTS_REJECTED', 'CREDIT_CHECK_FAILED', 'DEFAULTED']
export const managerRejectedFilter = (apps: LoanApplication[]) => apps.filter((a) => REJECTED_QUEUE_STATUSES.includes(a.status))
