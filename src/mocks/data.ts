import type {
  User,
  ApplicantProfile,
  LoanApplication,
  LoanProduct,
  LoanDocument,
  CreditCheck,
  Approval,
  Disbursement,
  AppNotification,
  AuditLogEntry,
  LoanStatus,
} from '@/types'
import { createRng, pick, intBetween, daysAgo, uid } from './random'
import { fullName, FIRST_NAMES, LAST_NAMES, DISTRICTS } from './names'
import { AVATAR_PALETTE } from '@/lib/avatarPalette'

const rng = createRng(20260903)

export const DEMO_PASSWORD = 'Passw0rd1'

export const staffUsers: User[] = [
  { id: 'staff-01', email: 'grace.officer@bankloan.ug', fullName: 'Grace Namutebi', phone: '0772345671', role: 'LOAN_OFFICER', branch: 'Mbarara Branch', avatarColor: AVATAR_PALETTE[0], createdAt: daysAgo(rng, 200, 400) },
  { id: 'staff-02', email: 'ronald.officer@bankloan.ug', fullName: 'Ronald Okello', phone: '0772345672', role: 'LOAN_OFFICER', branch: 'Kampala Branch', avatarColor: AVATAR_PALETTE[1], createdAt: daysAgo(rng, 200, 400) },
  { id: 'staff-03', email: 'sarah.analyst@bankloan.ug', fullName: 'Sarah Atim', phone: '0772345673', role: 'CREDIT_ANALYST', branch: 'Mbarara Branch', avatarColor: AVATAR_PALETTE[2], createdAt: daysAgo(rng, 200, 400) },
  { id: 'staff-04', email: 'david.analyst@bankloan.ug', fullName: 'David Mugisha', phone: '0772345674', role: 'CREDIT_ANALYST', branch: 'Kampala Branch', avatarColor: AVATAR_PALETTE[3], createdAt: daysAgo(rng, 200, 400) },
  { id: 'staff-05', email: 'esther.manager@bankloan.ug', fullName: 'Esther Kirabo', phone: '0772345675', role: 'BRANCH_MANAGER', branch: 'Mbarara Branch', avatarColor: AVATAR_PALETTE[4], createdAt: daysAgo(rng, 200, 400) },
  { id: 'staff-06', email: 'peter.manager@bankloan.ug', fullName: 'Peter Ssekandi', phone: '0772345676', role: 'BRANCH_MANAGER', branch: 'Kampala Branch', avatarColor: AVATAR_PALETTE[5], createdAt: daysAgo(rng, 200, 400) },
  { id: 'staff-07', email: 'admin@bankloan.ug', fullName: 'Josephine Nabirye', phone: '0772345677', role: 'ADMIN', branch: 'Head Office', avatarColor: AVATAR_PALETTE[0], createdAt: daysAgo(rng, 200, 400) },
]

export const loanProducts: LoanProduct[] = [
  { id: 'prod-personal', name: 'Personal Loan', type: 'PERSONAL', interestRateAnnual: 19, minAmount: 500_000, maxAmount: 15_000_000, minTermMonths: 3, maxTermMonths: 36, active: true },
  { id: 'prod-business', name: 'Business Growth Loan', type: 'BUSINESS', interestRateAnnual: 21, minAmount: 2_000_000, maxAmount: 80_000_000, minTermMonths: 6, maxTermMonths: 60, active: true },
  { id: 'prod-agri', name: 'Agriculture Loan', type: 'AGRICULTURE', interestRateAnnual: 16, minAmount: 500_000, maxAmount: 25_000_000, minTermMonths: 3, maxTermMonths: 24, active: true },
  { id: 'prod-asset', name: 'Asset Finance', type: 'ASSET_FINANCE', interestRateAnnual: 18, minAmount: 3_000_000, maxAmount: 60_000_000, minTermMonths: 12, maxTermMonths: 48, active: true },
  { id: 'prod-emergency', name: 'Emergency Loan', type: 'EMERGENCY', interestRateAnnual: 24, minAmount: 200_000, maxAmount: 5_000_000, minTermMonths: 1, maxTermMonths: 12, active: true },
]

const EMPLOYERS = ['MTN Uganda', 'Stanbic Bank', 'Mbarara University', 'Uganda Revenue Authority', 'Self-employed', 'Centenary Bank', 'Kampala City Traders', 'Riham Beverages', 'Nile Breweries', 'Local Farmer Cooperative']
const JOB_TITLES = ['Accountant', 'Teacher', 'Nurse', 'Shop Owner', 'Driver', 'Engineer', 'Sales Executive', 'Farmer', 'Civil Servant', 'Mechanic']
const EMPLOYMENT_TYPES: ApplicantProfile['employmentInfo']['employmentType'][] = ['FORMAL', 'SELF_EMPLOYED', 'INFORMAL']

export const borrowers: { user: User; profile: ApplicantProfile }[] = Array.from({ length: 40 }, (_, i) => {
  const name = fullName(rng, FIRST_NAMES, LAST_NAMES)
  const userId = uid('borrower', i + 1)
  return {
    user: {
      id: userId,
      email: `${name.toLowerCase().replace(/\s+/g, '.')}${i}@mail.com`,
      fullName: name,
      phone: `07${intBetween(rng, 7, 9)}${String(intBetween(rng, 0, 9999999)).padStart(7, '0')}`,
      role: 'BORROWER',
      avatarColor: pick(rng, AVATAR_PALETTE),
      createdAt: daysAgo(rng, 30, 500),
    },
    profile: {
      id: uid('profile', i + 1),
      userId,
      nationalId: `CM${intBetween(rng, 1000000, 9999999)}${pick(rng, ['A', 'B', 'C', 'D'])}${intBetween(rng, 10, 99)}`,
      dateOfBirth: daysAgo(rng, 365 * 19, 365 * 58).slice(0, 10),
      employmentInfo: { employer: pick(rng, EMPLOYERS), jobTitle: pick(rng, JOB_TITLES), employmentType: pick(rng, EMPLOYMENT_TYPES) },
      incomeMonthly: intBetween(rng, 1, 40) * 250_000,
      address: { district: pick(rng, DISTRICTS), city: pick(rng, DISTRICTS), street: `Plot ${intBetween(rng, 1, 200)}` },
    },
  }
})

export const applicantProfiles: ApplicantProfile[] = borrowers.map((b) => b.profile)
export function getApplicantProfile(applicantId: string): ApplicantProfile | undefined {
  return applicantProfiles.find((p) => p.userId === applicantId)
}

const STATUS_COUNTS: [LoanStatus, number][] = [
  ['DRAFT', 3], ['SUBMITTED', 6], ['PENDING_DOCUMENTS', 4], ['DOCUMENTS_REJECTED', 2],
  ['CREDIT_CHECK_PENDING', 5], ['CREDIT_CHECK_FAILED', 3], ['CREDIT_CHECK_PASSED', 2],
  ['UNDER_REVIEW', 7], ['AWAITING_ADDITIONAL_INFO', 4], ['APPROVED', 3], ['REJECTED', 6],
  ['PENDING_DISBURSEMENT', 2], ['DISBURSED', 4], ['ACTIVE', 9], ['DELINQUENT', 2],
  ['CLOSED', 5], ['DEFAULTED', 1],
]

const EARLY_STAGE: LoanStatus[] = ['DRAFT', 'SUBMITTED', 'PENDING_DOCUMENTS', 'DOCUMENTS_REJECTED', 'CREDIT_CHECK_PENDING', 'CREDIT_CHECK_FAILED', 'CREDIT_CHECK_PASSED', 'UNDER_REVIEW', 'AWAITING_ADDITIONAL_INFO']
const MID_STAGE: LoanStatus[] = ['APPROVED', 'REJECTED', 'PENDING_DISBURSEMENT']
const OFFICER_FLOW: LoanStatus[] = ['SUBMITTED', 'UNDER_REVIEW', 'AWAITING_ADDITIONAL_INFO', 'APPROVED', 'REJECTED', 'PENDING_DISBURSEMENT', 'DISBURSED', 'ACTIVE', 'DELINQUENT', 'CLOSED', 'DEFAULTED']
const HAS_CREDIT_CHECK: LoanStatus[] = ['CREDIT_CHECK_FAILED', 'CREDIT_CHECK_PASSED', 'UNDER_REVIEW', 'AWAITING_ADDITIONAL_INFO', 'APPROVED', 'REJECTED', 'PENDING_DISBURSEMENT', 'DISBURSED', 'ACTIVE', 'DELINQUENT', 'CLOSED', 'DEFAULTED']
const HAS_APPROVAL: LoanStatus[] = ['APPROVED', 'REJECTED', 'PENDING_DISBURSEMENT', 'DISBURSED', 'ACTIVE', 'DELINQUENT', 'CLOSED', 'DEFAULTED']
const HAS_DISBURSEMENT: LoanStatus[] = ['PENDING_DISBURSEMENT', 'DISBURSED', 'ACTIVE', 'DELINQUENT', 'CLOSED', 'DEFAULTED']
const PURPOSES = ['Business expansion', 'Home renovation', 'School fees', 'Medical expenses', 'Purchase of farm inputs', 'Vehicle purchase', 'Working capital', 'Wedding expenses', 'Land purchase', 'Debt consolidation']
const DOC_TYPES: LoanDocument['docType'][] = ['NATIONAL_ID', 'PAYSLIP', 'BANK_STATEMENT', 'PASSPORT_PHOTO']
const DISBURSE_METHODS: Disbursement['method'][] = ['BANK_TRANSFER', 'MOBILE_MONEY']

const officers = staffUsers.filter((u) => u.role === 'LOAN_OFFICER')
const managers = staffUsers.filter((u) => u.role === 'BRANCH_MANAGER')

export const applications: LoanApplication[] = []
export const documents: LoanDocument[] = []
export const creditChecks: CreditCheck[] = []
export const approvals: Approval[] = []
export const disbursements: Disbursement[] = []

let appCounter = 0
for (const [status, count] of STATUS_COUNTS) {
  for (let i = 0; i < count; i++) {
    appCounter++
    const borrower = pick(rng, borrowers)
    const product = pick(rng, loanProducts)
    const amount = intBetween(rng, product.minAmount / 100_000, product.maxAmount / 100_000) * 100_000
    const term = intBetween(rng, product.minTermMonths, product.maxTermMonths)
    const submittedDaysAgo = EARLY_STAGE.includes(status)
      ? intBetween(rng, 0, 45)
      : MID_STAGE.includes(status)
        ? intBetween(rng, 10, 90)
        : intBetween(rng, 30, 270)
    const submittedAt = daysAgo(rng, submittedDaysAgo, submittedDaysAgo)
    const updatedAt = daysAgo(rng, 0, submittedDaysAgo)
    const id = uid('LN', appCounter)
    const assignedOfficerId =
      OFFICER_FLOW.includes(status) && (status !== 'SUBMITTED' || rng() > 0.5) ? pick(rng, officers).id : undefined

    applications.push({
      id,
      applicantId: borrower.user.id,
      applicantName: borrower.user.fullName,
      applicantPhone: borrower.user.phone,
      productId: product.id,
      productType: product.type,
      amountRequested: amount,
      amountApproved: HAS_APPROVAL.includes(status) && status !== 'REJECTED' ? amount : undefined,
      termMonths: term,
      purpose: pick(rng, PURPOSES),
      status,
      assignedOfficerId,
      submittedAt,
      updatedAt,
    })

    if (status !== 'DRAFT') {
      const numDocs = intBetween(rng, 2, 4)
      for (let d = 0; d < numDocs; d++) {
        let verificationStatus: LoanDocument['verificationStatus']
        if (status === 'PENDING_DOCUMENTS') verificationStatus = d === 0 ? 'REJECTED' : 'PENDING'
        else if (status === 'DOCUMENTS_REJECTED') verificationStatus = 'REJECTED'
        else if (status === 'SUBMITTED') verificationStatus = 'PENDING'
        else verificationStatus = 'VERIFIED'

        const docType = DOC_TYPES[d % DOC_TYPES.length]
        documents.push({
          id: uid('DOC', documents.length + 1),
          loanApplicationId: id,
          docType,
          fileName: `${docType.toLowerCase()}_${id}.pdf`,
          verificationStatus,
          uploadedAt: submittedAt,
        })
      }
    }

    if (HAS_CREDIT_CHECK.includes(status)) {
      const passed = status !== 'CREDIT_CHECK_FAILED'
      creditChecks.push({
        id: uid('CC', creditChecks.length + 1),
        loanApplicationId: id,
        bureauReference: `CB-${intBetween(rng, 100000, 999999)}`,
        score: passed ? intBetween(rng, 620, 820) : intBetween(rng, 300, 549),
        result: passed ? 'PASSED' : 'FAILED',
        checkedAt: updatedAt,
      })
    }

    if (HAS_APPROVAL.includes(status)) {
      approvals.push({
        id: uid('APR', approvals.length + 1),
        loanApplicationId: id,
        decidedBy: pick(rng, managers).id,
        decision: status === 'REJECTED' ? 'REJECTED' : 'APPROVED',
        comments:
          status === 'REJECTED'
            ? 'Debt-to-income ratio exceeds policy threshold.'
            : 'Meets lending criteria; approved as requested.',
        decidedAt: updatedAt,
      })
    }

    if (HAS_DISBURSEMENT.includes(status)) {
      disbursements.push({
        id: uid('DIS', disbursements.length + 1),
        loanApplicationId: id,
        amount,
        method: pick(rng, DISBURSE_METHODS),
        status: status === 'PENDING_DISBURSEMENT' ? 'PENDING' : 'COMPLETED',
        disbursedAt: status === 'PENDING_DISBURSEMENT' ? undefined : updatedAt,
      })
    }
  }
}

applications.sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())

export const auditLog: AuditLogEntry[] = []
approvals.forEach((a) => {
  const staff = staffUsers.find((s) => s.id === a.decidedBy)
  if (!staff) return
  auditLog.push({
    id: uid('AUD', auditLog.length + 1),
    entityType: 'loan_application',
    entityId: a.loanApplicationId,
    action: a.decision === 'APPROVED' ? 'APPROVE_APPLICATION' : 'REJECT_APPLICATION',
    performedBy: staff.id,
    performedByName: staff.fullName,
    performedAt: a.decidedAt,
    details: a.comments,
  })
})
;[
  { action: 'CREATE_STAFF', entityType: 'user', details: 'Created staff account for Ronald Okello (LOAN_OFFICER)' },
  { action: 'UPDATE_PRODUCT', entityType: 'loan_product', details: 'Updated interest rate for Business Growth Loan to 21%' },
  { action: 'CREATE_PRODUCT', entityType: 'loan_product', details: 'Added new loan product: Emergency Loan' },
  { action: 'LOGIN', entityType: 'user', details: 'Successful login to staff dashboard' },
  { action: 'UPDATE_STAFF', entityType: 'user', details: "Updated branch assignment for David Mugisha to Kampala Branch" },
].forEach((entry) => {
  const admin = pick(rng, staffUsers.filter((s) => s.role === 'ADMIN'))
  auditLog.push({
    id: uid('AUD', auditLog.length + 1),
    entityType: entry.entityType,
    entityId: uid('ref', auditLog.length + 1),
    action: entry.action,
    performedBy: admin.id,
    performedByName: admin.fullName,
    performedAt: daysAgo(rng, 1, 60),
    details: entry.details,
  })
})
auditLog.sort((a, b) => new Date(b.performedAt).getTime() - new Date(a.performedAt).getTime())

export const notifications: AppNotification[] = applications.slice(0, 20).map((app, i) => ({
  id: uid('NOTE', i + 1),
  userId: app.applicantId,
  loanApplicationId: app.id,
  message: `Your application ${app.id} status changed to "${app.status.replace(/_/g, ' ').toLowerCase()}"`,
  sentAt: app.updatedAt,
  readAt: rng() > 0.4 ? app.updatedAt : undefined,
}))
