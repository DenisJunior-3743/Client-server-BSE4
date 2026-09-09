// Deterministic PRNG (mulberry32) so mock data — and the charts built from
// it — stay stable across reloads/hot-reload instead of reshuffling.
export function createRng(seed: number) {
  let a = seed
  return function rng() {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

export function pick<T>(rng: () => number, items: readonly T[]): T {
  return items[Math.floor(rng() * items.length)]
}

export function intBetween(rng: () => number, min: number, max: number): number {
  return Math.floor(rng() * (max - min + 1)) + min
}

export function daysAgo(rng: () => number, minDays: number, maxDays: number): string {
  const days = intBetween(rng, minDays, maxDays)
  const d = new Date()
  d.setDate(d.getDate() - days)
  d.setHours(intBetween(rng, 8, 17), intBetween(rng, 0, 59), 0, 0)
  return d.toISOString()
}

export function uid(prefix: string, index: number): string {
  return `${prefix}-${String(index).padStart(4, '0')}`
}
