import { applications, documents, creditChecks, approvals, disbursements, getApplicantProfile } from '@/mocks/data'
import type { ApplicationDetail, Disbursement, LoanApplication, LoanDocument, LoanStatus } from '@/types'
import { delay } from '../client'

export interface ApplicationFilter {
  statuses?: LoanStatus[]
}

export async function listApplications(filter?: ApplicationFilter): Promise<LoanApplication[]> {
  await delay(300)
  if (!filter?.statuses) return [...applications]
  return applications.filter((a) => filter.statuses!.includes(a.status))
}

export async function getApplicationDetail(id: string): Promise<ApplicationDetail> {
  await delay(300)
  const application = applications.find((a) => a.id === id)
  if (!application) throw new Error('Application not found')
  return {
    application,
    documents: documents.filter((d) => d.loanApplicationId === id),
    creditCheck: creditChecks.find((c) => c.loanApplicationId === id),
    approval: approvals.find((a) => a.loanApplicationId === id),
    disbursement: disbursements.find((d) => d.loanApplicationId === id),
    applicantProfile: getApplicantProfile(application.applicantId),
  }
}

// Mutations act on the in-memory mock array directly — Task 1 has no backend
// or persistence yet, so "saving" only lasts for the current browser session.
export async function updateApplicationStatus(
  id: string,
  status: LoanStatus,
  _options?: { version?: number; note?: string },
): Promise<LoanApplication> {
  await delay(400)
  const application = applications.find((a) => a.id === id)
  if (!application) throw new Error('Application not found')
  application.status = status
  application.updatedAt = new Date().toISOString()
  return application
}

export async function recordDecision(
  id: string,
  decision: 'APPROVED' | 'REJECTED',
  comments: string,
  _version?: number,
): Promise<LoanApplication> {
  await delay(400)
  const application = applications.find((a) => a.id === id)
  if (!application) throw new Error('Application not found')
  application.status = decision
  application.updatedAt = new Date().toISOString()
  if (decision === 'APPROVED') application.amountApproved = application.amountRequested
  approvals.push({
    id: `APR-${String(approvals.length + 1).padStart(4, '0')}`,
    loanApplicationId: id,
    decidedBy: 'current-user',
    decision,
    comments,
    decidedAt: application.updatedAt,
  })
  return application
}

export async function recordCreditCheck(
  id: string,
  score: number,
  result: 'PASSED' | 'FAILED',
  _version?: number,
): Promise<LoanApplication> {
  await delay(400)
  const application = applications.find((a) => a.id === id)
  if (!application) throw new Error('Application not found')
  application.status = result === 'PASSED' ? 'CREDIT_CHECK_PASSED' : 'CREDIT_CHECK_FAILED'
  application.updatedAt = new Date().toISOString()
  creditChecks.push({
    id: `CC-${String(creditChecks.length + 1).padStart(4, '0')}`,
    loanApplicationId: id,
    bureauReference: `CB-${Math.floor(100000 + Math.random() * 900000)}`,
    score,
    result,
    checkedAt: application.updatedAt,
  })
  return application
}

export async function setDocumentStatus(
  documentId: string,
  status: 'VERIFIED' | 'REJECTED',
  _version?: number,
): Promise<LoanDocument> {
  await delay(300)
  const document = documents.find((d) => d.id === documentId)
  if (!document) throw new Error('Document not found')
  document.verificationStatus = status
  return document
}

export async function assignToMe(id: string, officerId: string, _version?: number): Promise<LoanApplication> {
  await delay(300)
  const application = applications.find((a) => a.id === id)
  if (!application) throw new Error('Application not found')
  application.assignedOfficerId = officerId
  application.updatedAt = new Date().toISOString()
  return application
}

export async function disburseLoan(
  id: string,
  method: Disbursement['method'],
  _version?: number,
  _idempotencyKey?: string,
): Promise<Disbursement> {
  await delay(400)
  const application = applications.find((a) => a.id === id)
  if (!application) throw new Error('Application not found')
  application.status = 'ACTIVE'
  application.updatedAt = new Date().toISOString()
  const disbursement: Disbursement = {
    id: `DIS-${String(disbursements.length + 1).padStart(4, '0')}`,
    loanApplicationId: id,
    amount: application.amountApproved ?? application.amountRequested,
    method,
    status: 'COMPLETED',
    disbursedAt: application.updatedAt,
  }
  disbursements.push(disbursement)
  return disbursement
}
