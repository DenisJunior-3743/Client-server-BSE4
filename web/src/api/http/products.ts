import type { LoanProduct } from '@/types'
import { request } from '../client'

export function listProducts(): Promise<LoanProduct[]> {
  return request<LoanProduct[]>('/api/v1/products')
}

export type NewProductInput = Omit<LoanProduct, 'id' | 'active'>

export function createProduct(input: NewProductInput): Promise<LoanProduct> {
  return request<LoanProduct>('/api/v1/products', { method: 'POST', body: input })
}
