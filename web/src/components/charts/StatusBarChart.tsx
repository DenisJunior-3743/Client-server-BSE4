import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { StatusCount } from '@/lib/analytics'
import { TONE_HEX, CHART_GRID, CHART_AXIS_TEXT, CHART_TOOLTIP_STYLE } from '@/lib/chartColors'

export function StatusBarChart({ data }: { data: StatusCount[] }) {
  return (
    <ResponsiveContainer width="100%" height={Math.max(220, data.length * 34)}>
      <BarChart data={data} layout="vertical" margin={{ left: 8, right: 24, top: 4, bottom: 4 }}>
        <CartesianGrid horizontal={false} stroke={CHART_GRID} />
        <XAxis type="number" allowDecimals={false} tick={{ fontSize: 12, fill: CHART_AXIS_TEXT }} axisLine={false} tickLine={false} />
        <YAxis type="category" dataKey="label" width={150} tick={{ fontSize: 12, fill: '#334155' }} axisLine={false} tickLine={false} />
        <Tooltip cursor={{ fill: '#f1f5f9' }} contentStyle={CHART_TOOLTIP_STYLE} />
        <Bar dataKey="count" radius={[0, 6, 6, 0]} barSize={16}>
          {data.map((entry) => (
            <Cell key={entry.status} fill={TONE_HEX[entry.tone]} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}
