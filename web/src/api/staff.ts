import { USE_MOCKS } from './client'
import * as http from './http/staff'
import * as mock from './mock/staff'

export type { NewStaffInput } from './http/staff'

export const listStaff = USE_MOCKS ? mock.listStaff : http.listStaff
export const createStaff = USE_MOCKS ? mock.createStaff : http.createStaff
