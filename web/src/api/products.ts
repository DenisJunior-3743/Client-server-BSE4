import { loanProducts } from '@/mocks/data'
import type { LoanProduct } from '@/types'
import { delay } from './client'

export async function listProducts(): Promise<LoanProduct[]> {
  await delay(300)
  return [...loanProducts]
}

export type NewProductInput = Omit<LoanProduct, 'id' | 'active'>

export async function createProduct(input: NewProductInput): Promise<LoanProduct> {
  await delay(400)
  const product: LoanProduct = {
    id: `prod-${loanProducts.length + 1}`,
    active: true,
    ...input,
  }
  loanProducts.push(product)
  return product
}
