import { Button } from './Button'

export function LoadError({ message }: { message: string }) {
  return <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-5 text-sm text-red-800">
    <p>{message}</p>
    <Button type="button" variant="secondary" className="mt-3" onClick={() => window.location.reload()}>Try again</Button>
  </div>
}
