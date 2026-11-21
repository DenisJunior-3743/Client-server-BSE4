import { useEffect, useId, useState } from 'react'
import { Bell } from 'lucide-react'
import { Link } from 'react-router-dom'
import { request } from '@/api/client'
import { formatDateTime } from '@/lib/format'

interface StaffNotification {
  id: string
  loanApplicationId: string | null
  message: string
  sentAt: string
  readAt: string | null
}

export function StaffNotifications() {
  const panelId = useId()
  const [open, setOpen] = useState(false)
  const [notifications, setNotifications] = useState<StaffNotification[] | null>(null)
  const [error, setError] = useState('')
  const [marking, setMarking] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    let current = true
    void request<StaffNotification[]>('/api/v1/me/notifications')
      .then((items) => { if (current) setNotifications(items) })
      .catch(() => { if (current) setError('Could not load notifications. Close and reopen to retry.') })
    return () => { current = false }
  }, [open])

  async function markRead(id: string) {
    setMarking(id)
    setError('')
    try {
      const updated = await request<StaffNotification>(
        `/api/v1/me/notifications/${encodeURIComponent(id)}/read`, { method: 'POST' },
      )
      setNotifications((items) => items?.map((item) => item.id === id ? updated : item) ?? null)
    } catch {
      setError('Could not mark the notification as read.')
    } finally {
      setMarking(null)
    }
  }

  return (
    <div className="relative" onKeyDown={(event) => {
      if (event.key === 'Escape') {
        setOpen(false)
        event.currentTarget.querySelector('button')?.focus()
      }
    }}>
      <button
        type="button"
        className="rounded-lg p-2 text-neutral-600 hover:bg-neutral-100"
        aria-label="Notifications"
        aria-expanded={open}
        aria-controls={open ? panelId : undefined}
        onClick={() => {
          if (!open) {
            setNotifications(null)
            setError('')
          }
          setOpen(!open)
        }}
      >
        <Bell size={20} aria-hidden="true" />
      </button>
      {open && (
        <section
          id={panelId}
          aria-label="Staff notifications"
          className="absolute right-0 top-full mt-2 max-h-96 w-80 max-w-[calc(100vw-2rem)] overflow-auto rounded-xl border border-neutral-200 bg-white p-4 shadow-lg"
        >
          <h2 className="mb-2 text-sm font-semibold text-neutral-900">Notifications</h2>
          {error && <p role="alert" className="text-sm text-danger-600">{error}</p>}
          {!error && notifications === null && <p role="status" className="text-sm text-neutral-500">Loading…</p>}
          {notifications?.length === 0 && <p className="text-sm text-neutral-500">No notifications yet.</p>}
          <ul className="divide-y divide-neutral-100">
            {notifications?.map((item) => (
              <li key={item.id} className="py-3 text-sm text-neutral-800">
                <p className={item.readAt ? '' : 'font-semibold'}>{item.message}</p>
                <p className="mt-1 text-xs text-neutral-500">{formatDateTime(item.sentAt)}</p>
                <div className="mt-2 flex flex-wrap gap-3">
                  {item.loanApplicationId && (
                    <Link
                      className="font-medium text-primary-700 hover:underline"
                      to={`/applications/${encodeURIComponent(item.loanApplicationId)}`}
                      onClick={() => setOpen(false)}
                    >
                      Open loan
                    </Link>
                  )}
                  {!item.readAt && (
                    <button
                      type="button"
                      disabled={marking !== null}
                      className="font-medium text-primary-700 hover:underline disabled:opacity-50"
                      onClick={() => void markRead(item.id)}
                    >
                      Mark as read
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
