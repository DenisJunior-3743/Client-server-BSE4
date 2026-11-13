import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import LoginPage from '@/pages/LoginPage'
import { OverviewPage } from '@/pages/OverviewPage'
import { ApplicationsQueuePage } from '@/pages/ApplicationsQueuePage'
import ApplicationDetailPage from '@/pages/ApplicationDetailPage'
import ProfilePage from '@/pages/ProfilePage'
import StaffListPage from '@/pages/admin/StaffListPage'
import StaffFormPage from '@/pages/admin/StaffFormPage'
import ProductsListPage from '@/pages/admin/ProductsListPage'
import ProductFormPage from '@/pages/admin/ProductFormPage'
import AuditLogPage from '@/pages/admin/AuditLogPage'
import NotFoundPage from '@/pages/NotFoundPage'
import {
  officerNewFilter,
  officerInReviewFilter,
  officerInfoRequestsFilter,
  analystVerificationFilter,
  analystCreditAssessmentFilter,
  managerPendingApprovalFilter,
  managerApprovedFilter,
  managerRejectedFilter,
} from '@/lib/queues'

const OFFICER_SCOPE = ['SUBMITTED', 'PENDING_DOCUMENTS', 'DOCUMENTS_REJECTED', 'AWAITING_ADDITIONAL_INFO'] as const
const ANALYST_SCOPE = ['PENDING_DOCUMENTS', 'CREDIT_CHECK_PENDING', 'CREDIT_CHECK_FAILED', 'CREDIT_CHECK_PASSED'] as const
const MANAGER_SCOPE = ['UNDER_REVIEW', 'APPROVED', 'REJECTED', 'PENDING_DISBURSEMENT', 'DISBURSED'] as const

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route index element={<OverviewPage title="Bank-wide Overview" description="Portfolio snapshot across every branch and role." />} />

          <Route
            path="officer/overview"
            element={<OverviewPage title="Loan Officer Overview" description="Applications you're triaging before they move to review." scopeStatuses={[...OFFICER_SCOPE]} />}
          />
          <Route
            path="officer/new"
            element={<ApplicationsQueuePage title="New Applications" description="Freshly submitted, not yet picked up by an officer." filter={officerNewFilter} />}
          />
          <Route
            path="officer/in-review"
            element={<ApplicationsQueuePage title="In Review" description="Applications you're actively handling." filter={officerInReviewFilter} />}
          />
          <Route
            path="officer/info-requests"
            element={<ApplicationsQueuePage title="Info Requests" description="Waiting on the applicant to provide more information." filter={officerInfoRequestsFilter} />}
          />

          <Route
            path="analyst/overview"
            element={<OverviewPage title="Credit Analyst Overview" description="Document verification and credit checks in your queue." scopeStatuses={[...ANALYST_SCOPE]} />}
          />
          <Route
            path="analyst/verification"
            element={<ApplicationsQueuePage title="Pending Verification" description="Applications with documents awaiting verification." filter={analystVerificationFilter} />}
          />
          <Route
            path="analyst/credit-assessment"
            element={<ApplicationsQueuePage title="Credit Assessment" description="Documents verified — ready for a credit check." filter={analystCreditAssessmentFilter} />}
          />

          <Route
            path="manager/overview"
            element={<OverviewPage title="Branch Manager Overview" description="Final decisions and portfolio performance." scopeStatuses={[...MANAGER_SCOPE]} />}
          />
          <Route
            path="manager/pending-approval"
            element={<ApplicationsQueuePage title="Pending Approval" description="Passed credit check — awaiting your decision." filter={managerPendingApprovalFilter} />}
          />
          <Route
            path="manager/approved"
            element={<ApplicationsQueuePage title="Approved" description="Approved applications, through to disbursement and beyond." filter={managerApprovedFilter} />}
          />
          <Route
            path="manager/rejected"
            element={<ApplicationsQueuePage title="Rejected" description="Applications declined at any stage." filter={managerRejectedFilter} />}
          />

          <Route path="admin/overview" element={<OverviewPage title="Admin Overview" description="System-wide portfolio across all branches and roles." />} />
          <Route path="admin/staff" element={<StaffListPage />} />
          <Route path="admin/staff/new" element={<StaffFormPage />} />
          <Route path="admin/products" element={<ProductsListPage />} />
          <Route path="admin/products/new" element={<ProductFormPage />} />
          <Route path="admin/audit-log" element={<AuditLogPage />} />

          <Route path="applications/:id" element={<ApplicationDetailPage />} />
          <Route path="account/profile" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App
