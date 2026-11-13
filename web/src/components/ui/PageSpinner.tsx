export function PageSpinner() {
  return (
    <div className="flex min-h-[300px] items-center justify-center">
      <span className="h-8 w-8 animate-spin rounded-full border-[3px] border-primary-200 border-t-primary-600" />
    </div>
  )
}
