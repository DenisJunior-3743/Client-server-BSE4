import { useAuth } from '@/state/AuthContext'
import { STAFF_ROLE_META } from '@/lib/status'
import { Avatar } from '@/components/ui/Avatar'
import { StaffNotifications } from './StaffNotifications'
import { Menu } from 'lucide-react'

export function TopBar({ menuOpen, onOpenMenu }: { menuOpen: boolean; onOpenMenu: () => void }) {
  const { user } = useAuth()
  const today = new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

  return (
    <header className="relative z-10 flex h-16 shrink-0 items-center justify-between gap-2 border-b border-neutral-200 bg-white px-3 sm:px-6">
      <div className="flex min-w-0 items-center gap-2 lg:hidden">
        <button type="button" aria-label="Open menu" aria-haspopup="dialog" aria-controls="staff-menu"
          aria-expanded={menuOpen} onClick={onOpenMenu}
          className="grid size-11 shrink-0 place-items-center rounded-lg text-neutral-700 hover:bg-neutral-100 focus-visible:outline-2 focus-visible:outline-primary-500">
          <Menu size={22} aria-hidden="true" />
        </button>
        <span className="truncate font-display text-base font-bold text-neutral-900">TrustBank</span>
      </div>
      <p className="hidden text-sm font-medium text-neutral-500 lg:block">{today}</p>
      <div className="flex min-w-0 shrink-0 items-center gap-2 sm:gap-4">
        {user && <StaffNotifications key={user.id} />}
        {user && (
          <div className="flex items-center gap-2.5 border-l border-neutral-200 pl-2 sm:pl-4">
            <Avatar name={user.fullName} color={user.avatarColor} size={32} />
            <div className="hidden leading-tight sm:block">
              <p className="max-w-52 truncate text-sm font-semibold text-neutral-800">{user.fullName}</p>
              <p className="text-xs text-neutral-500">{STAFF_ROLE_META[user.role]?.label}</p>
            </div>
          </div>
        )}
      </div>
    </header>
  )
}
