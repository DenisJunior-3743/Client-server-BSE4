import type { StaffRole } from '@/types'
import {
  LayoutDashboard,
  UserCheck,
  FileStack,
  Clock,
  MessageCircleQuestion,
  ShieldCheck,
  FileSearch,
  BadgeCheck,
  Landmark,
  ListChecks,
  CheckCircle2,
  XCircle,
  Settings2,
  Users,
  Package,
  ScrollText,
  type LucideIcon,
} from 'lucide-react'

export interface NavChild {
  label: string
  to: string
  icon: LucideIcon
}

export interface NavSection {
  role: StaffRole
  label: string
  icon: LucideIcon
  children: NavChild[]
}

// Sidebar visibility and route access use the same staff-role policy.
export const NAV_SECTIONS: NavSection[] = [
  {
    role: 'LOAN_OFFICER',
    label: 'Loan Officer',
    icon: UserCheck,
    children: [
      { label: 'Overview & Stats', to: '/officer/overview', icon: LayoutDashboard },
      { label: 'New Applications', to: '/officer/new', icon: FileStack },
      { label: 'In Review', to: '/officer/in-review', icon: Clock },
      { label: 'Info Requests', to: '/officer/info-requests', icon: MessageCircleQuestion },
    ],
  },
  {
    role: 'CREDIT_ANALYST',
    label: 'Credit Analyst',
    icon: ShieldCheck,
    children: [
      { label: 'Overview & Stats', to: '/analyst/overview', icon: LayoutDashboard },
      { label: 'Pending Verification', to: '/analyst/verification', icon: FileSearch },
      { label: 'Credit Assessment', to: '/analyst/credit-assessment', icon: BadgeCheck },
    ],
  },
  {
    role: 'BRANCH_MANAGER',
    label: 'Branch Manager',
    icon: Landmark,
    children: [
      { label: 'Overview & Stats', to: '/manager/overview', icon: LayoutDashboard },
      { label: 'Pending Approval', to: '/manager/pending-approval', icon: ListChecks },
      { label: 'Approved', to: '/manager/approved', icon: CheckCircle2 },
      { label: 'Rejected', to: '/manager/rejected', icon: XCircle },
    ],
  },
  {
    role: 'ADMIN',
    label: 'Admin',
    icon: Settings2,
    children: [
      { label: 'Overview & Stats', to: '/admin/overview', icon: LayoutDashboard },
      { label: 'Staff Management', to: '/admin/staff', icon: Users },
      { label: 'Loan Products', to: '/admin/products', icon: Package },
      { label: 'Audit Log', to: '/admin/audit-log', icon: ScrollText },
    ],
  },
]
