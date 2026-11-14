export const money = (n: number) =>
  '$' + n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export const pad = (n: number) => String(n).padStart(2, '0')

export const isoDate = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

export const today = () => isoDate(new Date())

/** First day of the month and first day of the next month, as YYYY-MM-DD. */
export function monthRange(year: number, month: number): [string, string] {
  const next = month === 11 ? [year + 1, 0] : [year, month + 1]
  return [`${year}-${pad(month + 1)}-01`, `${next[0]}-${pad(next[1] + 1)}-01`]
}

export const monthLabel = (year: number, month: number) =>
  new Date(year, month, 1).toLocaleDateString(undefined, { month: 'long', year: 'numeric' })

export function dayLabel(iso: string): string {
  if (iso === today()) return 'Today'
  const y = new Date()
  y.setDate(y.getDate() - 1)
  if (iso === isoDate(y)) return 'Yesterday'
  const [yy, mm, dd] = iso.split('-').map(Number)
  return new Date(yy, mm - 1, dd).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' })
}

/** Parses "12.5", "$12.50" or "1,234.5" into a positive number rounded to cents, or null. */
export function parseAmount(raw: string): number | null {
  const cleaned = raw.replace(/[$,\s]/g, '')
  if (!/^\d*\.?\d*$/.test(cleaned) || cleaned === '' || cleaned === '.') return null
  const n = Math.round(Number(cleaned) * 100) / 100
  return n > 0 && n < 1e10 ? n : null
}
