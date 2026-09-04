import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuth } from '@/state/AuthContext'
import { profileSchema, type ProfileFormValues } from '@/lib/schemas'
import { STAFF_ROLE_META } from '@/lib/status'
import { formatDate } from '@/lib/format'
import { PageHeader } from '@/components/layout/PageHeader'
import { Card, CardHeader, CardTitle } from '@/components/ui/Card'
import { TextField } from '@/components/ui/TextField'
import { PhoneField } from '@/components/ui/PhoneField'
import { Button } from '@/components/ui/Button'
import { Avatar } from '@/components/ui/Avatar'
import { Badge } from '@/components/ui/Badge'

export default function ProfilePage() {
  const { user, updateProfile } = useAuth()
  const [saved, setSaved] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { fullName: user?.fullName ?? '', email: user?.email ?? '', phone: user?.phone ?? '' },
  })

  if (!user) return null

  async function onSubmit(values: ProfileFormValues) {
    updateProfile(values)
    setSaved(true)
    window.setTimeout(() => setSaved(false), 2500)
  }

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title="My Profile" description="Update your account details." />

      <Card className="mb-5">
        <div className="flex items-center gap-4">
          <Avatar name={user.fullName} color={user.avatarColor} size={56} />
          <div>
            <p className="font-display text-lg font-bold text-neutral-900">{user.fullName}</p>
            <div className="mt-1 flex items-center gap-2">
              <Badge tone="primary">{STAFF_ROLE_META[user.role]?.label}</Badge>
              <span className="text-sm text-neutral-500">{user.branch}</span>
            </div>
            <p className="mt-1 text-xs text-neutral-400">Staff since {formatDate(user.createdAt)}</p>
          </div>
        </div>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Account Details</CardTitle>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          <TextField label="Full name" required error={errors.fullName?.message} {...register('fullName')} />
          <TextField label="Email address" type="email" required error={errors.email?.message} {...register('email')} />
          <PhoneField label="Phone number" required error={errors.phone?.message} {...register('phone')} />

          <div className="flex items-center gap-3">
            <Button type="submit" isLoading={isSubmitting} disabled={!isDirty}>
              Save changes
            </Button>
            {saved && <span className="text-sm font-medium text-success-600">Saved.</span>}
          </div>
        </form>
      </Card>
    </div>
  )
}
