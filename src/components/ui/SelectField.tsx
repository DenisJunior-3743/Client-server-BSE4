import { forwardRef, type SelectHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

interface SelectFieldProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  error?: string
  hint?: string
  options: { value: string; label: string }[]
  placeholder?: string
}

export const SelectField = forwardRef<HTMLSelectElement, SelectFieldProps>(function SelectField(
  { label, error, hint, options, placeholder, className, id, required, ...props },
  ref,
) {
  const selectId = id ?? props.name
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={selectId} className="text-sm font-medium text-neutral-700">
        {label}
        {required && <span className="text-danger-600"> *</span>}
      </label>
      <select
        id={selectId}
        ref={ref}
        required={required}
        aria-invalid={!!error}
        defaultValue={props.defaultValue ?? ''}
        className={cn(
          'h-10 rounded-lg border bg-white px-3 text-sm text-neutral-900',
          'transition-shadow focus:outline-none focus:ring-4',
          error
            ? 'border-danger-400 focus:border-danger-500 focus:ring-danger-500/15'
            : 'border-neutral-300 focus:border-primary-500 focus:ring-primary-500/15',
          className,
        )}
        {...props}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {error ? (
        <p className="text-xs font-medium text-danger-600">{error}</p>
      ) : hint ? (
        <p className="text-xs text-neutral-500">{hint}</p>
      ) : null}
    </div>
  )
})
