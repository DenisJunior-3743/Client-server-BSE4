import { z } from 'zod'
import { isValidUgandaPhone, PHONE_ERROR_MESSAGE } from './phone'

export const phoneSchema = z.string().min(1, 'Phone number is required').refine(isValidUgandaPhone, PHONE_ERROR_MESSAGE)

export const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})
export type LoginFormValues = z.infer<typeof loginSchema>

export const staffSchema = z.object({
  fullName: z.string().min(1, 'Full name is required').min(2, 'Enter a full name'),
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  phone: phoneSchema,
  role: z.enum(['LOAN_OFFICER', 'CREDIT_ANALYST', 'BRANCH_MANAGER', 'ADMIN'], { error: 'Select a role' }),
  branch: z.string().min(1, 'Branch is required'),
})
export type StaffFormValues = z.infer<typeof staffSchema>

export const productSchema = z
  .object({
    name: z.string().min(1, 'Product name is required'),
    type: z.enum(['PERSONAL', 'BUSINESS', 'AGRICULTURE', 'ASSET_FINANCE', 'EMERGENCY'], { error: 'Select a type' }),
    interestRateAnnual: z.coerce.number({ error: 'Enter a valid interest rate' }).positive('Must be greater than 0').max(100, 'Must be 100 or less'),
    minAmount: z.coerce.number({ error: 'Enter a valid amount' }).positive('Must be greater than 0'),
    maxAmount: z.coerce.number({ error: 'Enter a valid amount' }).positive('Must be greater than 0'),
    minTermMonths: z.coerce.number({ error: 'Enter a valid term' }).int('Must be a whole number').positive('Must be greater than 0'),
    maxTermMonths: z.coerce.number({ error: 'Enter a valid term' }).int('Must be a whole number').positive('Must be greater than 0'),
  })
  .refine((data) => data.minAmount <= data.maxAmount, { message: 'Minimum cannot exceed maximum', path: ['maxAmount'] })
  .refine((data) => data.minTermMonths <= data.maxTermMonths, { message: 'Minimum term cannot exceed maximum term', path: ['maxTermMonths'] })
export type ProductFormValues = z.infer<typeof productSchema>

export const decisionSchema = z
  .object({
    decision: z.enum(['APPROVED', 'REJECTED'], { error: 'Select a decision' }),
    comments: z.string().min(1, 'Remarks are required'),
  })
  .refine((data) => data.decision !== 'REJECTED' || data.comments.trim().length >= 10, {
    message: 'Provide at least 10 characters explaining the rejection',
    path: ['comments'],
  })
export type DecisionFormValues = z.infer<typeof decisionSchema>

export const creditAssessmentSchema = z.object({
  score: z.coerce
    .number({ error: 'Enter a credit score' })
    .min(300, 'Score must be between 300 and 850')
    .max(850, 'Score must be between 300 and 850'),
  result: z.enum(['PASSED', 'FAILED'], { error: 'Select a result' }),
})
export type CreditAssessmentFormValues = z.infer<typeof creditAssessmentSchema>

export const infoRequestSchema = z.object({
  message: z.string().min(10, 'Describe what additional info is needed (min 10 characters)'),
})
export type InfoRequestFormValues = z.infer<typeof infoRequestSchema>

export const profileSchema = z.object({
  fullName: z.string().min(2, 'Enter a full name'),
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  phone: phoneSchema,
})
export type ProfileFormValues = z.infer<typeof profileSchema>
