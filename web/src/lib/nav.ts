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
  label: string
  icon: LucideIcon
  children: NavChild[]
}

// One parent per staff domain, per the plan: every domain's first child is
// always a graphical Overview & Stats page. All sections show for everyone
// in Task 1 — role-based filtering is a later phase, not built here.
export const NAV_SECTIONS: NavSection[] = [
  {
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
    label: 'Credit Analyst',
    icon: ShieldCheck,
    children: [
      { label: 'Overview & Stats', to: '/analyst/overview', icon: LayoutDashboard },
      { label: 'Pending Verification', to: '/analyst/verification', icon: FileSearch },
      { label: 'Credit Assessment', to: '/analyst/credit-assessment', icon: BadgeCheck },
    ],
  },
  {
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
