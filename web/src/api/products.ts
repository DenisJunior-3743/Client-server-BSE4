import { USE_MOCKS } from './client'
import * as http from './http/products'
import * as mock from './mock/products'

export type { NewProductInput } from './http/products'

export const listProducts = USE_MOCKS ? mock.listProducts : http.listProducts
export const createProduct = USE_MOCKS ? mock.createProduct : http.createProduct
