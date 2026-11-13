import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { ChevronDown, LayoutDashboard, LogOut, Landmark as BrandIcon } from 'lucide-react'
import { NAV_SECTIONS } from '@/lib/nav'
import { STAFF_ROLE_META } from '@/lib/status'
import { cn } from '@/lib/cn'
import { useAuth } from '@/state/AuthContext'
import { Avatar } from '@/components/ui/Avatar'

export function Sidebar() {
  const location = useLocation()
  const { user, logout } = useAuth()

  const [expanded, setExpanded] = useState<Set<string>>(() => {
    const active = NAV_SECTIONS.find((s) => s.children.some((c) => location.pathname.startsWith(c.to)))
    return new Set([active?.label ?? NAV_SECTIONS[0].label])
  })

  function toggle(label: string) {
    setExpanded((prev) => {
      const next = new Set(prev)
      if (next.has(label)) next.delete(label)
      else next.add(label)
      return next
    })
  }

  return (
    <aside className="flex h-screen w-[272px] shrink-0 flex-col bg-neutral-900 text-neutral-300">
      <div className="flex items-center gap-2.5 px-5 py-5">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary-600 text-white">
          <BrandIcon className="h-5 w-5" />
        </div>
        <div className="leading-tight">
          <p className="font-display text-sm font-bold text-white">TrustBank</p>
          <p className="text-xs text-neutral-400">Loan Dashboard</p>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 pb-4">
        <NavLink
          to="/"
          end
          className={({ isActive }) =>
            cn(
              'mb-3 flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
              isActive ? 'bg-primary-600 text-white' : 'text-neutral-300 hover:bg-neutral-800 hover:text-white',
            )
          }
        >
          <LayoutDashboard className="h-5 w-5" />
          Overview
        </NavLink>

        <p className="px-3 pb-1.5 pt-2 text-[11px] font-semibold uppercase tracking-wider text-neutral-500">
          Staff Workspaces
        </p>

        <div className="flex flex-col gap-1">
          {NAV_SECTIONS.map((section) => {
            const isExpanded = expanded.has(section.label)
            const SectionIcon = section.icon
            const sectionActive = section.children.some((c) => location.pathname.startsWith(c.to))
            return (
              <div key={section.label}>
                <button
                  type="button"
                  onClick={() => toggle(section.label)}
                  className={cn(
                    'flex w-full cursor-pointer items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                    sectionActive ? 'text-white' : 'text-neutral-300 hover:bg-neutral-800 hover:text-white',
                  )}
                >
                  <SectionIcon className="h-5 w-5" />
                  <span className="flex-1 text-left">{section.label}</span>
                  <ChevronDown className={cn('h-4 w-4 transition-transform', isExpanded && 'rotate-180')} />
                </button>
                {isExpanded && (
                  <div className="ml-4 mt-0.5 flex flex-col gap-0.5 border-l border-neutral-800 pl-4">
                    {section.children.map((child) => (
                      <NavLink
                        key={child.to}
                        to={child.to}
                        className={({ isActive }) =>
                          cn(
                            'flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors',
                            isActive
                              ? 'bg-primary-600 font-medium text-white'
                              : 'text-neutral-400 hover:bg-neutral-800 hover:text-white',
                          )
                        }
                      >
                        <child.icon className="h-4 w-4" />
                        {child.label}
                      </NavLink>
                    ))}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </nav>

      <div className="border-t border-neutral-800 p-3">
        <NavLink
          to="/account/profile"
          className={({ isActive }) =>
            cn('flex items-center gap-3 rounded-lg px-2 py-2 transition-colors', isActive ? 'bg-neutral-800' : 'hover:bg-neutral-800')
          }
        >
          {user && <Avatar name={user.fullName} color={user.avatarColor} size={34} />}
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium text-white">{user?.fullName}</p>
            <p className="truncate text-xs text-neutral-400">{user && STAFF_ROLE_META[user.role]?.label}</p>
          </div>
        </NavLink>
        <button
          type="button"
          onClick={logout}
          className="mt-1 flex w-full cursor-pointer items-center gap-3 rounded-lg px-3 py-2 text-sm text-neutral-400 transition-colors hover:bg-neutral-800 hover:text-white"
        >
          <LogOut className="h-4 w-4" />
          Log out
        </button>
      </div>
    </aside>
  )
}
