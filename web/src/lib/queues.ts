import { documents } from '@/mocks/data'
import type { LoanApplication, LoanStatus } from '@/types'

// Composite queue filters used by the ApplicationsQueuePage instances in
// App.tsx — kept here (rather than inline) because a couple of them need to
// cross-reference the documents collection, not just the application itself.
export const officerNewFilter = (apps: LoanApplication[]) => apps.filter((a) => a.status === 'SUBMITTED' && !a.assignedOfficerId)

export const officerInReviewFilter = (apps: LoanApplication[]) => apps.filter((a) => a.status === 'SUBMITTED' && !!a.assignedOfficerId)

export const officerInfoRequestsFilter = (apps: LoanApplication[]) => apps.filter((a) => a.status === 'AWAITING_ADDITIONAL_INFO')

export const analystVerificationFilter = (apps: LoanApplication[]) =>
  apps.filter((a) => documents.some((d) => d.loanApplicationId === a.id && d.verificationStatus === 'PENDING'))

export const analystCreditAssessmentFilter = (apps: LoanApplication[]) => apps.filter((a) => a.status === 'CREDIT_CHECK_PENDING')

export const managerPendingApprovalFilter = (apps: LoanApplication[]) => apps.filter((a) => a.status === 'UNDER_REVIEW')

const APPROVED_QUEUE_STATUSES: LoanStatus[] = ['APPROVED', 'PENDING_DISBURSEMENT', 'DISBURSED', 'ACTIVE', 'CLOSED']
export const managerApprovedFilter = (apps: LoanApplication[]) => apps.filter((a) => APPROVED_QUEUE_STATUSES.includes(a.status))

const REJECTED_QUEUE_STATUSES: LoanStatus[] = ['REJECTED', 'DOCUMENTS_REJECTED', 'CREDIT_CHECK_FAILED', 'DEFAULTED']
export const managerRejectedFilter = (apps: LoanApplication[]) => apps.filter((a) => REJECTED_QUEUE_STATUSES.includes(a.status))
