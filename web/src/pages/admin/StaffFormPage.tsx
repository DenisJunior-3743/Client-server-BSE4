import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowLeft } from 'lucide-react'
import { createStaff, getStaff, updateStaff } from '@/api/staff'
import type { User } from '@/types'
import { useAuth } from '@/state/AuthContext'
import { isStaffRole } from '@/lib/access'
import { LoadError } from '@/components/ui/LoadError'
import { PageSpinner } from '@/components/ui/PageSpinner'
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
  const { id } = useParams<{ id: string }>()
  return id ? <StaffEditor key={id} id={id} /> : <StaffForm />
}

function StaffEditor({ id }: { id: string }) {
  const [staff, setStaff] = useState<User | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    getStaff(id).then((member) => { if (!cancelled) setStaff(member) })
      .catch((error: unknown) => { if (!cancelled) setLoadError(error instanceof Error ? error.message : 'Unable to load staff member') })
    return () => { cancelled = true }
  }, [id])

  if (loadError) return <LoadError message={loadError} />
  if (!staff || staff.id !== id) return <PageSpinner />
  return <StaffForm key={id} staff={staff} />
}

function StaffForm({ staff }: { staff?: User }) {
  const navigate = useNavigate()
  const { user, updateProfile } = useAuth()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<StaffFormValues>({
    resolver: zodResolver(staffSchema),
    defaultValues: {
      fullName: staff?.fullName ?? '', email: staff?.email ?? '', phone: staff?.phone ?? '', branch: staff?.branch ?? '',
      role: staff && isStaffRole(staff.role) ? staff.role : undefined,
    },
  })

  const [temporaryPassword, setTemporaryPassword] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  async function onSubmit(values: StaffFormValues) {
    setFormError(null)
    try {
      if (staff) {
        const updated = await updateStaff(staff.id, values)
        if (updated.id === user?.id) updateProfile(updated)
        navigate('/admin/staff')
        return
      }
      const created = await createStaff(values)
      // The real API returns a one-time temporary password; show it before leaving the page.
      if (created.temporaryPassword) setTemporaryPassword(created.temporaryPassword)
      else navigate('/admin/staff')
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors) {
        for (const field of ['fullName', 'email', 'phone', 'role', 'branch'] as const) {
          const message = err.fieldErrors[field]
          if (message) setError(field, { message })
        }
      }
      setFormError(err instanceof Error ? err.message : 'Could not save the account')
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
      <PageHeader
        title={staff ? 'Edit Staff Member' : 'Add Staff Member'}
        description={staff ? 'Update account details and staff role. The password stays the same.' : 'Create a new bank staff account.'}
      />
      <Card>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          <TextField label="Full name" required placeholder="e.g. Grace Namutebi" error={errors.fullName?.message} {...register('fullName')} />
          <TextField label="Email address" type="email" required placeholder="name@bankloan.ug" error={errors.email?.message} {...register('email')} />
          <PhoneField label="Phone number" required error={errors.phone?.message} {...register('phone')} />
          <SelectField label="Role" required placeholder="Select a role" options={staff?.id === user?.id ? ROLE_OPTIONS.filter((option) => option.value === 'ADMIN') : ROLE_OPTIONS} error={errors.role?.message} {...register('role')} />
          <TextField label="Branch" required placeholder="e.g. Mbarara Branch" error={errors.branch?.message} {...register('branch')} />
          {formError && <p role="alert" className="text-sm font-medium text-danger-600">{formError}</p>}
          <Button type="submit" isLoading={isSubmitting}>
            {staff ? 'Save changes' : 'Create staff account'}
          </Button>
        </form>
      </Card>
    </div>
  )
}
