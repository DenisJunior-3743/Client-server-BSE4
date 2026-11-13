// Uganda mobile numbers as entered by staff/applicants in this system:
// local format only, strictly "07" + 8 more digits (10 digits total).
export const PHONE_MAX_LENGTH = 10
export const PHONE_PATTERN = /^07\d{8}$/
export const PHONE_ERROR_MESSAGE = 'Phone number must start with 07 and have exactly 10 digits'

/**
 * Keeps only digits and caps the length at 10 — used inside the PhoneInput's
 * onChange so a user can never type (or paste) a letter/symbol, or a digit
 * past the 10th, into the field in the first place.
 */
export function sanitizePhoneInput(raw: string): string {
  return raw.replace(/\D/g, '').slice(0, PHONE_MAX_LENGTH)
}

export function isValidUgandaPhone(value: string): boolean {
  return PHONE_PATTERN.test(value)
}
