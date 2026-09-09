import type { StatusTone } from './status'

// Same hex values as the Tailwind color tokens in index.css — Recharts can't
// consume CSS variables directly, so this is the one place they're repeated.
export const TONE_HEX: Record<StatusTone, string> = {
  neutral: '#94a3b8',
  primary: '#3b82f6',
  warning: '#f59e0b',
  success: '#10b981',
  danger: '#ef4444',
}

export const CHART_GRID = '#e2e8f0'
export const CHART_AXIS_TEXT = '#64748b'
export const CHART_TOOLTIP_STYLE = {
  borderRadius: 12,
  border: '1px solid #e2e8f0',
  fontSize: 12,
  boxShadow: '0 10px 30px -6px rgb(15 23 42 / 0.18)',
}
