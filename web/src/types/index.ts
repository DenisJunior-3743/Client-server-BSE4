// Staff dashboard types for the Spring Boot API.

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
  // Workflow metadata returned by the backend:
  reference?: string
  /** Optimistic-lock version. Send it back with every command on this application. */
  version?: number
  pendingDocuments?: number
  offerExpiresAt?: string
  decisionRemarks?: string
  infoRequestMessage?: string
}

export interface LoanDocument {
  id: string
  loanApplicationId: string
  downloadUrl?: string | null
  contentType?: string
  sizeBytes?: number
  docType: 'NATIONAL_ID' | 'PAYSLIP' | 'BANK_STATEMENT' | 'COLLATERAL_PROOF' | 'PASSPORT_PHOTO'
  fileName: string
  verificationStatus: 'PENDING' | 'VERIFIED' | 'REJECTED'
  uploadedAt: string
  version?: number
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

export interface StatusHistoryEntry {
  fromStatus?: LoanStatus
  toStatus: LoanStatus
  note?: string
  changedAt: string
}

/** Everything the application detail page shows, in one round trip. */
export interface ApplicationDetail {
  application: LoanApplication
  documents: LoanDocument[]
  creditCheck?: CreditCheck
  approval?: Approval
  disbursement?: Disbursement
  applicantProfile?: ApplicantProfile
  history?: StatusHistoryEntry[]
}

export interface StaffCreated {
  user: User
  /** Generated passwords are shown once; null when the admin supplied the initial password. */
  temporaryPassword?: string | null
}
