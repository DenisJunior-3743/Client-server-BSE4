import { USE_MOCKS } from './client'
import * as http from './http/applications'
import * as mock from './mock/applications'

export type { ApplicationFilter } from './http/applications'

export const listApplications = USE_MOCKS ? mock.listApplications : http.listApplications
export const getApplicationDetail = USE_MOCKS ? mock.getApplicationDetail : http.getApplicationDetail
export const updateApplicationStatus = USE_MOCKS ? mock.updateApplicationStatus : http.updateApplicationStatus
export const recordDecision = USE_MOCKS ? mock.recordDecision : http.recordDecision
export const recordCreditCheck = USE_MOCKS ? mock.recordCreditCheck : http.recordCreditCheck
export const setDocumentStatus = USE_MOCKS ? mock.setDocumentStatus : http.setDocumentStatus
export const assignToMe = USE_MOCKS ? mock.assignToMe : http.assignToMe
export const disburseLoan = USE_MOCKS ? mock.disburseLoan : http.disburseLoan
