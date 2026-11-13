import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Package, PlusCircle } from 'lucide-react'
import { listProducts } from '@/api/products'
import type { LoanProduct } from '@/types'
import { formatUGX } from '@/lib/format'
import { PageHeader } from '@/components/layout/PageHeader'
import { PageSpinner } from '@/components/ui/PageSpinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'

const PRODUCT_LABELS: Record<LoanProduct['type'], string> = {
  PERSONAL: 'Personal',
  BUSINESS: 'Business',
  AGRICULTURE: 'Agriculture',
  ASSET_FINANCE: 'Asset Finance',
  EMERGENCY: 'Emergency',
}

export default function ProductsListPage() {
  const [products, setProducts] = useState<LoanProduct[] | null>(null)

  useEffect(() => {
    listProducts().then(setProducts)
  }, [])

  if (!products) return <PageSpinner />

  return (
    <div>
      <PageHeader
        title="Loan Products"
        description="Products available for applicants to apply against."
        actions={
          <Link to="/admin/products/new">
            <Button type="button">
              <PlusCircle className="h-4 w-4" /> Add product
            </Button>
          </Link>
        }
      />
      {products.length === 0 ? (
        <EmptyState icon={Package} title="No loan products yet" />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {products.map((p) => (
            <Card key={p.id}>
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-display text-base font-bold text-neutral-900">{p.name}</p>
                  <Badge tone={p.active ? 'success' : 'neutral'}>{p.active ? 'Active' : 'Inactive'}</Badge>
                </div>
                <span className="rounded-lg bg-primary-50 px-2 py-1 text-xs font-semibold text-primary-700">{PRODUCT_LABELS[p.type]}</span>
              </div>
              <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
                <div>
                  <dt className="text-xs text-neutral-400">Interest rate</dt>
                  <dd className="font-medium text-neutral-800">{p.interestRateAnnual}% p.a.</dd>
                </div>
                <div>
                  <dt className="text-xs text-neutral-400">Term range</dt>
                  <dd className="font-medium text-neutral-800">
                    {p.minTermMonths}–{p.maxTermMonths} months
                  </dd>
                </div>
                <div className="col-span-2">
                  <dt className="text-xs text-neutral-400">Amount range</dt>
                  <dd className="font-medium text-neutral-800">
                    {formatUGX(p.minAmount)} – {formatUGX(p.maxAmount)}
                  </dd>
                </div>
              </dl>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
