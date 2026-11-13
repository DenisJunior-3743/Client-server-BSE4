import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'
import type { StatusTone } from '@/lib/status'

const TONE_CLASSES: Record<StatusTone, string> = {
  neutral: 'bg-neutral-100 text-neutral-700 ring-neutral-200',
  primary: 'bg-primary-50 text-primary-700 ring-primary-200',
  warning: 'bg-warning-50 text-warning-700 ring-warning-200',
  success: 'bg-success-50 text-success-700 ring-success-200',
  danger: 'bg-danger-50 text-danger-700 ring-danger-200',
}

export function Badge({ tone = 'neutral', children }: { tone?: StatusTone; children: ReactNode }) {
  return (
    <span
      className={cn(
        'inline-flex w-fit items-center gap-1 whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset',
        TONE_CLASSES[tone],
      )}
    >
      {children}
    </span>
  )
}
