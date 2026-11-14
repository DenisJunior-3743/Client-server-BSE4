import { forwardRef, useId, useState, type InputHTMLAttributes } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { cn } from '@/lib/cn'

export interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  hint?: string
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(function TextField(
  { label, error, hint, className, id, required, type = 'text', disabled, ...props },
  ref,
) {
  const generatedId = useId()
  const inputId = id ?? props.name ?? generatedId
  const [passwordVisible, setPasswordVisible] = useState(false)
  const isPassword = type === 'password'
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={inputId} className="text-sm font-medium text-neutral-700">
        {label}
        {required && <span className="text-danger-600"> *</span>}
      </label>
      <div className={cn('relative', isPassword && 'min-w-32')}>
        <input
          id={inputId}
          ref={ref}
          required={required}
          type={isPassword && passwordVisible ? 'text' : type}
          disabled={disabled}
          aria-invalid={!!error}
          className={cn(
            'h-10 w-full rounded-lg border bg-white px-3 text-sm text-neutral-900 placeholder:text-neutral-400',
            'transition-shadow focus:outline-none focus:ring-4',
            error
              ? 'border-danger-400 focus:border-danger-500 focus:ring-danger-500/15'
              : 'border-neutral-300 focus:border-primary-500 focus:ring-primary-500/15',
            isPassword && 'pr-11', className,
          )}
          {...props}
        />
        {isPassword && (
          <button
            type="button"
            aria-label={`${passwordVisible ? 'Hide' : 'Show'} ${label.toLowerCase()}`}
            aria-pressed={passwordVisible}
            aria-controls={inputId}
            disabled={disabled}
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => setPasswordVisible((visible) => !visible)}
            className="absolute right-1 top-1/2 inline-flex h-8 w-8 -translate-y-1/2 cursor-pointer items-center justify-center rounded-md text-neutral-500 hover:bg-neutral-100 hover:text-neutral-800 focus-visible:outline-2 focus-visible:outline-primary-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {passwordVisible ? <EyeOff aria-hidden="true" className="h-4 w-4" /> : <Eye aria-hidden="true" className="h-4 w-4" />}
          </button>
        )}
      </div>
      {error ? (
        <p className="text-xs font-medium text-danger-600">{error}</p>
      ) : hint ? (
        <p className="text-xs text-neutral-500">{hint}</p>
      ) : null}
    </div>
  )
})
