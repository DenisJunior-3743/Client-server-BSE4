import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useLocation, useNavigate } from 'react-router-dom'
import { Landmark } from 'lucide-react'
import { useAuth } from '@/state/AuthContext'
import { loginSchema, type LoginFormValues } from '@/lib/schemas'
import { TextField } from '@/components/ui/TextField'
import { Button } from '@/components/ui/Button'

export default function LoginPage() {
  const { login, isLoading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  async function onSubmit(values: LoginFormValues) {
    setFormError(null)
    try {
      await login(values.email, values.password)
      const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/'
      navigate(from, { replace: true })
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Unable to log in')
    }
  }

  return (
    <div className="flex min-h-screen bg-neutral-50">
      <div className="relative hidden w-[46%] flex-col items-center justify-center overflow-hidden bg-gradient-to-br from-primary-700 via-primary-800 to-neutral-900 p-12 text-white lg:flex">
        <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-primary-500/30 blur-3xl" />
        <div className="absolute -bottom-32 -left-16 h-72 w-72 rounded-full bg-primary-400/20 blur-3xl" />

        <div className="relative flex flex-col items-center gap-4 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-white/15 backdrop-blur">
            <Landmark className="h-8 w-8" />
          </div>
          <div>
            <p className="font-display text-2xl font-bold">TrustBank</p>
            <p className="text-sm text-primary-100">Loan Dashboard</p>
          </div>
        </div>

        <p className="absolute bottom-12 text-xs text-primary-200">Client Server Programming for Applications · MUST</p>
      </div>

      <div className="flex flex-1 items-center justify-center p-6">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex flex-col items-center gap-2 lg:hidden">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-600 text-white">
              <Landmark className="h-6 w-6" />
            </div>
            <p className="font-display text-lg font-bold text-neutral-900">TrustBank Loan Dashboard</p>
          </div>

          <h2 className="font-display text-2xl font-bold text-neutral-900">Staff sign in</h2>
          <p className="mt-1 text-sm text-neutral-500">Use your bank staff account to access the dashboard.</p>

          <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-6 flex flex-col gap-4">
            <TextField
              label="Email address"
              type="email"
              placeholder="you@bankloan.ug"
              autoComplete="email"
              required
              error={errors.email?.message}
              {...register('email')}
            />
            <TextField
              label="Password"
              type="password"
              placeholder="••••••••"
              autoComplete="current-password"
              required
              error={errors.password?.message}
              {...register('password')}
            />

            {formError && <p className="rounded-lg bg-danger-50 px-3 py-2 text-sm font-medium text-danger-700">{formError}</p>}

            <Button type="submit" isLoading={isLoading} className="mt-2 w-full">
              Sign in
            </Button>
          </form>
        </div>
      </div>
    </div>
  )
}
