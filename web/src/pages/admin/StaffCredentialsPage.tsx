import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowLeft } from 'lucide-react'
import { getStaff, setStaffCredentials } from '@/api/staff'
import { ApiError } from '@/api/client'
import type { User } from '@/types'
import { useAuth } from '@/state/AuthContext'
import { staffCredentialsSchema, type StaffCredentialsFormValues } from '@/lib/schemas'
import { PageHeader } from '@/components/layout/PageHeader'
import { Card } from '@/components/ui/Card'
import { TextField } from '@/components/ui/TextField'
import { Button } from '@/components/ui/Button'
import { LoadError } from '@/components/ui/LoadError'
import { PageSpinner } from '@/components/ui/PageSpinner'

export default function StaffCredentialsPage() {
  const { id } = useParams<{ id: string }>()
  return id ? <CredentialsEditor key={id} id={id} /> : <LoadError message="Staff member not found" />
}

function CredentialsEditor({ id }: { id: string }) {
  const [staff, setStaff] = useState<User | null>(null)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => {
    let cancelled = false
    getStaff(id).then((member) => { if (!cancelled) setStaff(member) })
      .catch((err: unknown) => { if (!cancelled) setError(err instanceof Error ? err.message : 'Unable to load staff member') })
    return () => { cancelled = true }
  }, [id])
  if (error) return <LoadError message={error} />
  if (!staff) return <PageSpinner />
  return <CredentialsForm staff={staff} />
}

function CredentialsForm({ staff }: { staff: User }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, reset, setError: setFieldError, formState: { errors, isSubmitting } } = useForm<StaffCredentialsFormValues>({
    resolver: zodResolver(staffCredentialsSchema), defaultValues: { password: '', confirmPassword: '' },
  })

  async function onSubmit(values: StaffCredentialsFormValues) {
    setError(null)
    try {
      await setStaffCredentials(staff.id, values.password)
      reset()
      if (staff.id === user?.id) {
        logout()
        navigate('/login', { replace: true })
      } else {
        setSaved(true)
      }
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors?.password) setFieldError('password', { message: err.fieldErrors.password })
      setError(err instanceof Error ? err.message : 'Could not save the password')
    }
  }

  return (
    <div className="mx-auto max-w-xl">
      <Link to="/admin/staff" className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-neutral-500 hover:text-neutral-800">
        <ArrowLeft aria-hidden="true" className="h-4 w-4" /> Back to staff
      </Link>
      <PageHeader title="Staff Credentials" description={`Set or change the password for ${staff.fullName}. Email is the username.`} />
      <Card>
        {saved ? (
          <div role="status">
            <p className="text-sm text-success-700">Password saved. {staff.email} can sign in using the new password.</p>
            <Button type="button" className="mt-4" onClick={() => navigate('/admin/staff')}>Done</Button>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
            <TextField id="staff-username" label="Username (email)" type="email" autoComplete="username" readOnly value={staff.email} />
            <TextField label="New password" type="password" autoComplete="new-password" required
              hint="Use 8–72 characters, uppercase and lowercase letters, and a number."
              error={errors.password?.message} {...register('password')} />
            <TextField label="Confirm new password" type="password" autoComplete="new-password" required
              error={errors.confirmPassword?.message} {...register('confirmPassword')} />
            <p className="text-xs text-neutral-500">Existing sessions will be signed out. {staff.id === user?.id ? 'You will need to sign in again after saving.' : 'Only the password will change.'}</p>
            {error && <p role="alert" className="text-sm font-medium text-danger-600">{error}</p>}
            <Button type="submit" isLoading={isSubmitting}>Save password</Button>
          </form>
        )}
      </Card>
    </div>
  )
}
