import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

export interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  hint?: string
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(function TextField(
  { label, error, hint, className, id, required, ...props },
  ref,
) {
  const inputId = id ?? props.name
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={inputId} className="text-sm font-medium text-neutral-700">
        {label}
        {required && <span className="text-danger-600"> *</span>}
      </label>
      <input
        id={inputId}
        ref={ref}
        required={required}
        aria-invalid={!!error}
        className={cn(
          'h-10 rounded-lg border bg-white px-3 text-sm text-neutral-900 placeholder:text-neutral-400',
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
