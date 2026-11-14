import { LoadError } from '@/components/ui/LoadError'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { KeyRound, Pencil, Trash2, UserPlus, Users } from 'lucide-react'
import { deleteStaff, listStaff } from '@/api/staff'
import { useAuth } from '@/state/AuthContext'
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
  const { user } = useAuth()
  const [staff, setStaff] = useState<User[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  useEffect(() => {
    listStaff().then(setStaff).catch((error: unknown) => setLoadError(error instanceof Error ? error.message : 'Unable to load data'))
  }, [])

  async function handleDelete(member: User) {
    if (deletingId || member.id === user?.id) return
    if (!window.confirm(`Delete ${member.fullName}'s staff account? They will lose access. Existing loan and audit history will be retained.`)) return
    setActionError(null)
    setDeletingId(member.id)
    try {
      await deleteStaff(member.id)
      setStaff((current) => current?.filter((item) => item.id !== member.id) ?? null)
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Could not delete the staff account')
    } finally {
      setDeletingId(null)
    }
  }

  if (loadError) return <LoadError message={loadError} />
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
      {actionError && <p role="alert" className="mb-4 rounded-lg bg-danger-50 p-3 text-sm text-danger-700">{actionError}</p>}
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
                <th className="px-5 py-3 text-right font-semibold">Actions</th>
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
                  <td className="px-5 py-3.5">
                    <div className="flex justify-end gap-1">
                      <Link
                        to={`/admin/staff/${encodeURIComponent(s.id)}/edit`}
                        aria-label={`Edit ${s.fullName}`}
                        title="Edit staff"
                        className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-primary-600 hover:bg-primary-50 focus-visible:outline-2 focus-visible:outline-primary-500"
                      >
                        <Pencil aria-hidden="true" className="h-4 w-4" />
                      </Link>
                      <Link
                        to={`/admin/staff/${encodeURIComponent(s.id)}/credentials`}
                        aria-label={`Set password for ${s.fullName}`}
                        title="Set or change password"
                        className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-primary-600 hover:bg-primary-50 focus-visible:outline-2 focus-visible:outline-primary-500"
                      >
                        <KeyRound aria-hidden="true" className="h-4 w-4" />
                      </Link>
                      <Button
                        type="button" variant="ghost" size="sm"
                        aria-label={`Delete ${s.fullName}`}
                        title={s.id === user?.id ? 'You cannot delete your own account' : 'Delete staff'}
                        disabled={deletingId !== null || s.id === user?.id}
                        isLoading={deletingId === s.id}
                        onClick={() => void handleDelete(s)}
                        className="h-9 w-9 px-0 text-danger-600 hover:bg-danger-50"
                      >
                        {deletingId !== s.id && <Trash2 aria-hidden="true" className="h-4 w-4" />}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
