import { useEffect, useState } from 'react'
import { listProducts } from '@/api/products'
import type { LoanProduct } from '@/types'

/** Loan products for looking up a product name by id. Loaded from the backend. */
export function useProducts(): LoanProduct[] {
  const [products, setProducts] = useState<LoanProduct[]>([])
  useEffect(() => {
    let active = true
    listProducts()
      .then((data) => {
        if (active) setProducts(data)
      })
      .catch(() => {
        // The page still renders; it falls back to the product type label.
      })
    return () => {
      active = false
    }
  }, [])
  return products
}
