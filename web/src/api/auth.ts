import { USE_MOCKS } from './client'
import * as http from './http/auth'
import * as mock from './mock/auth'

export const login = USE_MOCKS ? mock.login : http.login
