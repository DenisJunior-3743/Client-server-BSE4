import { forwardRef, type TextareaHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

interface TextareaFieldProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string
  error?: string
  hint?: string
}

export const TextareaField = forwardRef<HTMLTextAreaElement, TextareaFieldProps>(function TextareaField(
  { label, error, hint, className, id, required, rows = 3, ...props },
  ref,
) {
  const textareaId = id ?? props.name
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={textareaId} className="text-sm font-medium text-neutral-700">
        {label}
        {required && <span className="text-danger-600"> *</span>}
      </label>
      <textarea
        id={textareaId}
        ref={ref}
        rows={rows}
        required={required}
        aria-invalid={!!error}
        className={cn(
          'resize-none rounded-lg border bg-white px-3 py-2 text-sm text-neutral-900 placeholder:text-neutral-400',
          'transition-shadow focus:outline-none focus:ring-4',
          error
            ? 'border-danger-400 focus:border-danger-500 focus:ring-danger-500/15'
            : 'border-neutral-300 focus:border-primary-500 focus:ring-primary-500/15',
          className,
        )}
        {...props}
      />
      {error ? (
        <p className="text-xs font-medium text-danger-600">{error}</p>
      ) : hint ? (
        <p className="text-xs text-neutral-500">{hint}</p>
      ) : null}
    </div>
  )
})
