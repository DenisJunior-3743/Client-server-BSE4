// Mirrors diagrams/erd.puml field-for-field so swapping mock data for the
// real API in Phase 2 is a data-layer change, not a type rewrite.

export type StaffRole = 'LOAN_OFFICER' | 'CREDIT_ANALYST' | 'BRANCH_MANAGER' | 'ADMIN'
export type UserRole = 'BORROWER' | StaffRole

export interface User {
  id: string
  email: string
  fullName: string
  phone: string
  role: UserRole
  branch?: string
  avatarColor: string
  createdAt: string
}

export interface ApplicantProfile {
  id: string
  userId: string
  nationalId: string
  dateOfBirth: string
  employmentInfo: {
    employer: string
    jobTitle: string
    employmentType: 'FORMAL' | 'SELF_EMPLOYED' | 'INFORMAL'
  }
  incomeMonthly: number
  address: {
    district: string
    city: string
    street: string
  }
}

// Mirrors diagrams/loan-workflow-state.puml
export type LoanStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'PENDING_DOCUMENTS'
  | 'DOCUMENTS_REJECTED'
  | 'CREDIT_CHECK_PENDING'
  | 'CREDIT_CHECK_FAILED'
  | 'CREDIT_CHECK_PASSED'
  | 'UNDER_REVIEW'
  | 'AWAITING_ADDITIONAL_INFO'
  | 'APPROVED'
  | 'REJECTED'
  | 'PENDING_DISBURSEMENT'
  | 'DISBURSED'
  | 'ACTIVE'
  | 'DELINQUENT'
  | 'CLOSED'
  | 'DEFAULTED'

export interface LoanProduct {
  id: string
  name: string
  type: 'PERSONAL' | 'BUSINESS' | 'AGRICULTURE' | 'ASSET_FINANCE' | 'EMERGENCY'
  interestRateAnnual: number
  minAmount: number
  maxAmount: number
  minTermMonths: number
  maxTermMonths: number
  active: boolean
}

export interface LoanApplication {
  id: string
  applicantId: string
  applicantName: string
  applicantPhone: string
  productId: string
  productType: LoanProduct['type']
  amountRequested: number
  amountApproved?: number
  termMonths: number
  purpose: string
  status: LoanStatus
  assignedOfficerId?: string
  submittedAt: string
  updatedAt: string
}

export interface LoanDocument {
  id: string
  loanApplicationId: string
  docType: 'NATIONAL_ID' | 'PAYSLIP' | 'BANK_STATEMENT' | 'COLLATERAL_PROOF' | 'PASSPORT_PHOTO'
  fileName: string
  verificationStatus: 'PENDING' | 'VERIFIED' | 'REJECTED'
  uploadedAt: string
}

export interface CreditCheck {
  id: string
  loanApplicationId: string
  bureauReference: string
  score: number
  result: 'PASSED' | 'FAILED'
  checkedAt: string
}

export interface Approval {
  id: string
  loanApplicationId: string
  decidedBy: string
  decision: 'APPROVED' | 'REJECTED'
  comments: string
  decidedAt: string
}

export interface Disbursement {
  id: string
  loanApplicationId: string
  amount: number
  method: 'BANK_TRANSFER' | 'MOBILE_MONEY'
  status: 'PENDING' | 'COMPLETED'
  disbursedAt?: string
}

export interface AppNotification {
  id: string
  userId: string
  loanApplicationId?: string
  message: string
  sentAt: string
  readAt?: string
}

export interface AuditLogEntry {
  id: string
  entityType: string
  entityId: string
  action: string
  performedBy: string
  performedByName: string
  performedAt: string
  details: string
}
