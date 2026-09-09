import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { UserPlus, Users } from 'lucide-react'
import { listStaff } from '@/api/staff'
import type { User } from '@/types'
import { STAFF_ROLE_META } from '@/lib/status'
import { formatDate } from '@/lib/format'
import { colorForId } from '@/lib/avatarPalette'
import { PageHeader } from '@/components/layout/PageHeader'
import { PageSpinner } from '@/components/ui/PageSpinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Avatar } from '@/components/ui/Avatar'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'

export default function StaffListPage() {
  const [staff, setStaff] = useState<User[] | null>(null)

  useEffect(() => {
    listStaff().then(setStaff)
  }, [])

  if (!staff) return <PageSpinner />

  return (
    <div>
      <PageHeader
        title="Staff Management"
        description="Bank staff accounts across all branches."
        actions={
          <Link to="/admin/staff/new">
            <Button type="button">
              <UserPlus className="h-4 w-4" /> Add staff
            </Button>
          </Link>
        }
      />
      {staff.length === 0 ? (
        <EmptyState icon={Users} title="No staff yet" />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-neutral-200 bg-white shadow-[var(--shadow-card)]">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead>
              <tr className="border-b border-neutral-200 bg-neutral-50 text-xs uppercase tracking-wide text-neutral-500">
                <th className="px-5 py-3 font-semibold">Name</th>
                <th className="px-5 py-3 font-semibold">Role</th>
                <th className="px-5 py-3 font-semibold">Branch</th>
                <th className="px-5 py-3 font-semibold">Phone</th>
                <th className="px-5 py-3 font-semibold">Joined</th>
              </tr>
            </thead>
            <tbody>
              {staff.map((s) => (
                <tr key={s.id} className="border-b border-neutral-100 last:border-0">
                  <td className="px-5 py-3.5">
                    <div className="flex items-center gap-3">
                      <Avatar name={s.fullName} color={colorForId(s.id)} size={32} />
                      <div>
                        <p className="font-medium text-neutral-900">{s.fullName}</p>
                        <p className="text-xs text-neutral-500">{s.email}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-5 py-3.5">
                    <Badge tone="primary">{STAFF_ROLE_META[s.role]?.label}</Badge>
                  </td>
                  <td className="px-5 py-3.5 text-neutral-600">{s.branch}</td>
                  <td className="px-5 py-3.5 text-neutral-600">{s.phone}</td>
                  <td className="px-5 py-3.5 text-neutral-600">{formatDate(s.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
