// Simulated network latency so loading states are visible during Task 1.
// Swap this whole folder's internals for real fetch/axios calls in Phase 2 —
// callers (pages/hooks) won't need to change since the function signatures stay put.
export function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
