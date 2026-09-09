import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/cn'

interface StatCardProps {
  label: string
  value: string
  icon: LucideIcon
  tone?: 'primary' | 'success' | 'warning' | 'danger'
  hint?: string
}

const TONE_CLASSES: Record<NonNullable<StatCardProps['tone']>, string> = {
  primary: 'bg-primary-50 text-primary-600',
  success: 'bg-success-50 text-success-600',
  warning: 'bg-warning-50 text-warning-600',
  danger: 'bg-danger-50 text-danger-600',
}

export function StatCard({ label, value, icon: Icon, tone = 'primary', hint }: StatCardProps) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-[var(--shadow-card)] transition-shadow hover:shadow-[var(--shadow-card-hover)]">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-neutral-500">{label}</p>
          <p className="mt-1.5 font-display text-2xl font-extrabold text-neutral-900">{value}</p>
          {hint && <p className="mt-1 truncate text-xs text-neutral-500">{hint}</p>}
        </div>
        <div className={cn('flex h-10 w-10 shrink-0 items-center justify-center rounded-xl', TONE_CLASSES[tone])}>
          <Icon className="h-5 w-5" strokeWidth={2.25} />
        </div>
      </div>
    </div>
  )
}
