import type { ComponentProps } from 'react'
import { CartesianGrid } from 'recharts'
import { CHART_GRID } from '@/lib/chartColors'

type GridProps = ComponentProps<typeof CartesianGrid>
type VerticalGenerator = NonNullable<GridProps['verticalCoordinatesGenerator']>
type HorizontalGenerator = NonNullable<GridProps['horizontalCoordinatesGenerator']>
type GridAxis = Parameters<VerticalGenerator>[0]['xAxis']

// Recharts' default grid calls text measurement without font metrics. Grid
// lines only need scaled tick positions; labels retain their own measurement.
function coordinates(axis: GridAxis): number[] {
  const scale = axis?.scale
  if (!scale) return []
  const values = axis.ticks ?? axis.niceTicks ?? scale.ticks?.(axis.tickCount ?? 5) ?? scale.domain()
  return values.map((value) => scale.map(value, { position: 'middle' }))
    .filter((coordinate): coordinate is number => typeof coordinate === 'number' && Number.isFinite(coordinate))
}
const verticalCoordinates: VerticalGenerator = ({ xAxis }) => coordinates(xAxis)
const horizontalCoordinates: HorizontalGenerator = ({ yAxis }) => coordinates(yAxis)

export function ChartGrid({ horizontal = true, vertical = true }: { horizontal?: boolean; vertical?: boolean }) {
  return <CartesianGrid horizontal={horizontal} vertical={vertical} stroke={CHART_GRID}
    horizontalCoordinatesGenerator={horizontalCoordinates} verticalCoordinatesGenerator={verticalCoordinates} />
}
