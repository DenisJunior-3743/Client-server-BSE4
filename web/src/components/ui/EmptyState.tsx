import type { LucideIcon } from 'lucide-react'

export function EmptyState({ icon: Icon, title, description }: { icon: LucideIcon; title: string; description?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-neutral-300 bg-neutral-50/60 py-16 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-neutral-100 text-neutral-400">
        <Icon className="h-6 w-6" />
      </div>
      <div>
        <p className="text-sm font-semibold text-neutral-700">{title}</p>
        {description && <p className="mx-auto mt-1 max-w-sm text-sm text-neutral-500">{description}</p>}
      </div>
    </div>
  )
}
