import type { ApplicationDetail, Disbursement, LoanApplication, LoanDocument, LoanStatus } from '@/types'
import { request } from '../client'

export interface ApplicationFilter {
  statuses?: LoanStatus[]
}

export function listApplications(filter?: ApplicationFilter): Promise<LoanApplication[]> {
  const query = filter?.statuses?.length ? `?status=${filter.statuses.join(',')}` : ''
  return request<LoanApplication[]>(`/api/v1/applications${query}`)
}

export function getApplicationDetail(id: string): Promise<ApplicationDetail> {
  return request<ApplicationDetail>(`/api/v1/applications/${id}`)
}

// Every command below sends the `version` this screen loaded. If another officer changed the
// application in the meantime the server answers 409 VERSION_CONFLICT (see isVersionConflict in
// ../client) instead of silently overwriting their work.

export function updateApplicationStatus(
  id: string,
  status: LoanStatus,
  options?: { version?: number; note?: string },
): Promise<LoanApplication> {
  return request<LoanApplication>(`/api/v1/applications/${id}/status`, {
    method: 'POST',
    body: { status, version: options?.version ?? 0, note: options?.note },
  })
}

export function recordDecision(
  id: string,
  decision: 'APPROVED' | 'REJECTED',
  comments: string,
  version?: number,
): Promise<LoanApplication> {
  return request<LoanApplication>(`/api/v1/applications/${id}/decision`, {
    method: 'POST',
    body: { decision, comments, version: version ?? 0 },
  })
}

export function recordCreditCheck(
  id: string,
  score: number,
  result: 'PASSED' | 'FAILED',
  version?: number,
): Promise<LoanApplication> {
  return request<LoanApplication>(`/api/v1/applications/${id}/credit-check`, {
    method: 'POST',
    body: { score, result, version: version ?? 0 },
  })
}

export function setDocumentStatus(
  documentId: string,
  status: 'VERIFIED' | 'REJECTED',
  version?: number,
): Promise<LoanDocument> {
  return request<LoanDocument>(`/api/v1/documents/${documentId}/verification`, {
    method: 'PATCH',
    body: { status, version: version ?? 0 },
  })
}

// The server takes the officer identity from the signed-in user token.
export function assignToMe(id: string, _officerId: string, version?: number): Promise<LoanApplication> {
  return request<LoanApplication>(`/api/v1/applications/${id}/assign`, {
    method: 'POST',
    body: { version: version ?? 0 },
  })
}

// Releasing money is the one staff command that must never run twice, so besides `version` it
// carries an Idempotency-Key. The caller keeps the SAME key for every retry of one disbursement:
// if the first request succeeded but its reply was lost, the retry gets that first reply back.
export function disburseLoan(
  id: string,
  method: Disbursement['method'],
  version?: number,
  idempotencyKey?: string,
): Promise<Disbursement> {
  return request<Disbursement>(`/api/v1/applications/${id}/disbursement`, {
    method: 'POST',
    body: { method, version: version ?? 0 },
    idempotencyKey,
  })
}
