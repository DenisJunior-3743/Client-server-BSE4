import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { CHART_TOOLTIP_STYLE } from '@/lib/chartColors'

interface DonutChartProps {
  data: { label: string; value: number; color: string }[]
}

export function DonutChart({ data }: DonutChartProps) {
  return (
    <ResponsiveContainer width="100%" height={200}>
      <PieChart>
        <Pie data={data} dataKey="value" nameKey="label" innerRadius={54} outerRadius={78} paddingAngle={3} strokeWidth={0}>
          {data.map((entry) => (
            <Cell key={entry.label} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip contentStyle={CHART_TOOLTIP_STYLE} />
      </PieChart>
    </ResponsiveContainer>
  )
}
