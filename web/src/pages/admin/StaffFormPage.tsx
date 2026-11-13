import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowLeft } from 'lucide-react'
import { createStaff } from '@/api/staff'
import { ApiError } from '@/api/client'
import { staffSchema, type StaffFormValues } from '@/lib/schemas'
import { PageHeader } from '@/components/layout/PageHeader'
import { Card } from '@/components/ui/Card'
import { TextField } from '@/components/ui/TextField'
import { PhoneField } from '@/components/ui/PhoneField'
import { SelectField } from '@/components/ui/SelectField'
import { Button } from '@/components/ui/Button'

const ROLE_OPTIONS = [
  { value: 'LOAN_OFFICER', label: 'Loan Officer' },
  { value: 'CREDIT_ANALYST', label: 'Credit Analyst' },
  { value: 'BRANCH_MANAGER', label: 'Branch Manager' },
  { value: 'ADMIN', label: 'Admin' },
]

export default function StaffFormPage() {
  const navigate = useNavigate()
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(staffSchema),
    defaultValues: { fullName: '', email: '', phone: '', branch: '' },
  })

  const [temporaryPassword, setTemporaryPassword] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  async function onSubmit(values: StaffFormValues) {
    setFormError(null)
    try {
      const created = await createStaff(values)
      // The real API returns a one-time temporary password; show it before leaving the page.
      if (created.temporaryPassword) setTemporaryPassword(created.temporaryPassword)
      else navigate('/admin/staff')
    } catch (err) {
      setFormError(err instanceof ApiError || err instanceof Error ? err.message : 'Could not create the account')
    }
  }

  if (temporaryPassword) {
    return (
      <div className="mx-auto max-w-xl">
        <PageHeader title="Staff account created" description="Give this temporary password to the new staff member. It is shown only once." />
        <Card>
          <p className="rounded-lg bg-neutral-50 p-4 text-center font-mono text-xl font-semibold tracking-wider text-neutral-900">
            {temporaryPassword}
          </p>
          <Button type="button" className="mt-4 w-full" onClick={() => navigate('/admin/staff')}>
            Done
          </Button>
        </Card>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-xl">
      <button
        type="button"
        onClick={() => navigate(-1)}
        className="mb-4 inline-flex cursor-pointer items-center gap-1.5 text-sm font-medium text-neutral-500 hover:text-neutral-800"
      >
        <ArrowLeft className="h-4 w-4" /> Back
      </button>
      <PageHeader title="Add Staff Member" description="Create a new bank staff account." />
      <Card>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          <TextField label="Full name" required placeholder="e.g. Grace Namutebi" error={errors.fullName?.message} {...register('fullName')} />
          <TextField label="Email address" type="email" required placeholder="name@bankloan.ug" error={errors.email?.message} {...register('email')} />
          <PhoneField label="Phone number" required error={errors.phone?.message} {...register('phone')} />
          <SelectField label="Role" required placeholder="Select a role" options={ROLE_OPTIONS} error={errors.role?.message} {...register('role')} />
          <TextField label="Branch" required placeholder="e.g. Mbarara Branch" error={errors.branch?.message} {...register('branch')} />
          {formError && <p className="text-sm font-medium text-danger-600">{formError}</p>}
          <Button type="submit" isLoading={isSubmitting}>
            Create staff account
          </Button>
        </form>
      </Card>
    </div>
  )
}
