import { useEffect, useRef, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { TopBar } from './TopBar'

export function DashboardLayout() {
  const menu = useRef<HTMLDialogElement>(null)
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    const desktop = window.matchMedia('(min-width: 64rem)')
    function onResize(event: MediaQueryListEvent) {
      if (event.matches) menu.current?.close()
    }
    desktop.addEventListener('change', onResize)
    return () => desktop.removeEventListener('change', onResize)
  }, [])

  function openMenu() {
    menu.current?.showModal()
    setMenuOpen(true)
  }

  function closeMenu() {
    menu.current?.close()
  }

  return (
    <div className="flex h-dvh bg-neutral-50">
      <div className="hidden shrink-0 lg:flex"><Sidebar /></div>
      <div className="flex min-w-0 flex-1 flex-col">
        <TopBar menuOpen={menuOpen} onOpenMenu={openMenu} />
        <main className="min-w-0 flex-1 overflow-auto px-3 py-4 sm:px-6 sm:py-6">
          <Outlet />
        </main>
      </div>
      <dialog
        id="staff-menu"
        ref={menu}
        aria-label="Staff menu"
        onClose={() => setMenuOpen(false)}
        onClick={(event) => { if (event.target === event.currentTarget) closeMenu() }}
        className="m-0 h-dvh max-h-none w-[min(17rem,calc(100vw-3rem))] max-w-none bg-neutral-900 p-0 backdrop:bg-neutral-950/60 lg:hidden"
      >
        <Sidebar onClose={closeMenu} />
      </dialog>
    </div>
  )
}
