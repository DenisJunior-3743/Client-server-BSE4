import { ApiError, request } from '../client'

type LoanReadMethod = 'loan.getApplication' | 'loan.getRepaymentSchedule'
type RpcResponse<T> = { jsonrpc: '2.0'; id: string } & (
  | { result: T }
  | { error: { code: number; message: string; data?: { status: number; code: string } } }
)

/** Marshal a loan query, correlate its reply, and translate errors for the existing dashboard. */
export async function readLoanRpc<T>(
  method: LoanReadMethod,
  applicationId: string,
  readFromRest: () => Promise<T>,
): Promise<T> {
  const id = crypto.randomUUID()
  let reply: RpcResponse<T>
  try {
    reply = await request<RpcResponse<T>>('/api/v1/rpc', {
      method: 'POST',
      readOnly: true,
      body: { jsonrpc: '2.0', method, params: { applicationId }, id },
    })
  } catch (ex) {
    // An older backend can serve the same read while the dashboard and API roll out independently.
    if (ex instanceof ApiError && (ex.status === 404 || ex.status === 405)) {
      return readFromRest()
    }
    throw ex
  }

  if (!reply || reply.jsonrpc !== '2.0' || reply.id !== id || ('result' in reply) === ('error' in reply)) {
    throw new ApiError(502, 'INVALID_RPC_RESPONSE', 'The banking service returned an unexpected response.')
  }
  if ('error' in reply) {
    if (!reply.error || !Number.isInteger(reply.error.code) || typeof reply.error.message !== 'string') {
      throw new ApiError(502, 'INVALID_RPC_RESPONSE', 'The banking service returned an unexpected response.')
    }
    const { code, message, data } = reply.error
    throw new ApiError(data?.status ?? 502, data?.code ?? `RPC_${code}`, message)
  }
  return reply.result
}
