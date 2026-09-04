import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { CHART_GRID, CHART_AXIS_TEXT, CHART_TOOLTIP_STYLE } from '@/lib/chartColors'

interface TrendAreaChartProps {
  data: { label: string; value: number }[]
  color: string
  valueFormatter?: (value: number) => string
}

export function TrendAreaChart({ data, color, valueFormatter }: TrendAreaChartProps) {
  const gradientId = `trend-${color.replace('#', '')}`
  return (
    <ResponsiveContainer width="100%" height={260}>
      <AreaChart data={data} margin={{ left: -12, right: 12, top: 8, bottom: 0 }}>
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity={0.35} />
            <stop offset="100%" stopColor={color} stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid vertical={false} stroke={CHART_GRID} />
        <XAxis dataKey="label" tick={{ fontSize: 12, fill: CHART_AXIS_TEXT }} axisLine={false} tickLine={false} />
        <YAxis tick={{ fontSize: 12, fill: CHART_AXIS_TEXT }} axisLine={false} tickLine={false} width={40} />
        <Tooltip
          formatter={(value) => (typeof value === 'number' && valueFormatter ? valueFormatter(value) : value)}
          contentStyle={CHART_TOOLTIP_STYLE}
        />
        <Area type="monotone" dataKey="value" stroke={color} strokeWidth={2.5} fill={`url(#${gradientId})`} />
      </AreaChart>
    </ResponsiveContainer>
  )
}
