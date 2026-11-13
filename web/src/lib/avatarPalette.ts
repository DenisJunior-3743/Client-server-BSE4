// Decorative variety for person avatars — separate from the semantic
// status-tone palette in chartColors.ts, which must stay meaningful.
export const AVATAR_PALETTE = ['#2563EB', '#059669', '#D97706', '#DC2626', '#7C3AED', '#0891B2']

export function colorForId(id: string): string {
  let hash = 0
  for (let i = 0; i < id.length; i++) hash = (hash * 31 + id.charCodeAt(i)) >>> 0
  return AVATAR_PALETTE[hash % AVATAR_PALETTE.length]
}
