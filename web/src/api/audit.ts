import { USE_MOCKS } from './client'
import * as http from './http/audit'
import * as mock from './mock/audit'

export const listAuditLog = USE_MOCKS ? mock.listAuditLog : http.listAuditLog
