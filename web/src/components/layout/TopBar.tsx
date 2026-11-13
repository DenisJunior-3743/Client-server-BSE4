import { useAuth } from '@/state/AuthContext'
import { STAFF_ROLE_META } from '@/lib/status'
import { Avatar } from '@/components/ui/Avatar'

export function TopBar() {
  const { user } = useAuth()
  const today = new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

  return (
    <header className="sticky top-0 z-10 flex h-16 shrink-0 items-center justify-between border-b border-neutral-200 bg-white/85 px-6 backdrop-blur">
      <p className="text-sm font-medium text-neutral-500">{today}</p>
      <div className="flex items-center gap-4">
        {user && (
          <div className="flex items-center gap-2.5 border-l border-neutral-200 pl-4">
            <Avatar name={user.fullName} color={user.avatarColor} size={32} />
            <div className="leading-tight">
              <p className="text-sm font-semibold text-neutral-800">{user.fullName}</p>
              <p className="text-xs text-neutral-500">{STAFF_ROLE_META[user.role]?.label}</p>
            </div>
          </div>
        )}
      </div>
    </header>
  )
}
