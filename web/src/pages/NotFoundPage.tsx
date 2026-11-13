import { Link } from 'react-router-dom'
import { Compass } from 'lucide-react'
import { Button } from '@/components/ui/Button'

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-neutral-50 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-50 text-primary-600">
        <Compass className="h-7 w-7" />
      </div>
      <div>
        <h1 className="font-display text-2xl font-extrabold text-neutral-900">Page not found</h1>
        <p className="mt-1 text-sm text-neutral-500">The page you're looking for doesn't exist.</p>
      </div>
      <Link to="/">
        <Button type="button">Back to dashboard</Button>
      </Link>
    </div>
  )
}
