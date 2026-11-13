import { forwardRef, type ChangeEvent } from 'react'
import { sanitizePhoneInput, PHONE_MAX_LENGTH } from '@/lib/phone'
import { TextField, type TextFieldProps } from './TextField'

type PhoneFieldProps = Omit<TextFieldProps, 'type' | 'maxLength'>

/**
 * A phone input that makes bad input impossible to type rather than just
 * flagging it after the fact: every keystroke/paste is stripped to digits
 * only and hard-capped at 10 characters before it ever reaches form state.
 */
export const PhoneField = forwardRef<HTMLInputElement, PhoneFieldProps>(function PhoneField(
  { onChange, ...props },
  ref,
) {
  function handleChange(e: ChangeEvent<HTMLInputElement>) {
    e.target.value = sanitizePhoneInput(e.target.value)
    onChange?.(e)
  }

  return (
    <TextField
      ref={ref}
      type="tel"
      inputMode="numeric"
      autoComplete="tel"
      placeholder="07XXXXXXXX"
      maxLength={PHONE_MAX_LENGTH}
      onChange={handleChange}
      {...props}
    />
  )
})
