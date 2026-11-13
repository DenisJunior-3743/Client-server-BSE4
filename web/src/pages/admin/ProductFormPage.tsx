import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowLeft } from 'lucide-react'
import { createProduct } from '@/api/products'
import { productSchema, type ProductFormValues } from '@/lib/schemas'
import { PageHeader } from '@/components/layout/PageHeader'
import { Card } from '@/components/ui/Card'
import { TextField } from '@/components/ui/TextField'
import { SelectField } from '@/components/ui/SelectField'
import { Button } from '@/components/ui/Button'

const TYPE_OPTIONS = [
  { value: 'PERSONAL', label: 'Personal' },
  { value: 'BUSINESS', label: 'Business' },
  { value: 'AGRICULTURE', label: 'Agriculture' },
  { value: 'ASSET_FINANCE', label: 'Asset Finance' },
  { value: 'EMERGENCY', label: 'Emergency' },
]

export default function ProductFormPage() {
  const navigate = useNavigate()
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(productSchema),
    defaultValues: { name: '' },
  })

  async function onSubmit(values: ProductFormValues) {
    await createProduct(values)
    navigate('/admin/products')
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
      <PageHeader title="Add Loan Product" description="Define a new product applicants can apply against." />
      <Card>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          <TextField label="Product name" required placeholder="e.g. Salary Advance" error={errors.name?.message} {...register('name')} />
          <SelectField label="Type" required placeholder="Select a type" options={TYPE_OPTIONS} error={errors.type?.message} {...register('type')} />
          <TextField
            label="Annual interest rate (%)"
            type="number"
            step="0.1"
            required
            placeholder="e.g. 19"
            error={errors.interestRateAnnual?.message}
            {...register('interestRateAnnual')}
          />
          <div className="grid grid-cols-2 gap-4">
            <TextField label="Minimum amount (UGX)" type="number" required placeholder="500000" error={errors.minAmount?.message} {...register('minAmount')} />
            <TextField label="Maximum amount (UGX)" type="number" required placeholder="15000000" error={errors.maxAmount?.message} {...register('maxAmount')} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <TextField label="Minimum term (months)" type="number" required placeholder="3" error={errors.minTermMonths?.message} {...register('minTermMonths')} />
            <TextField label="Maximum term (months)" type="number" required placeholder="36" error={errors.maxTermMonths?.message} {...register('maxTermMonths')} />
          </div>
          <Button type="submit" isLoading={isSubmitting}>
            Create product
          </Button>
        </form>
      </Card>
    </div>
  )
}
