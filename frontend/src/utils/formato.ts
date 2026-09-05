const fmtMoeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

type Numerico = number | string | null | undefined

function numero(v: Numerico): number {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

/** 18.5 → "R$ 18,50" */
export function moeda(v: Numerico): string {
  return fmtMoeda.format(numero(v))
}

/** 12 → "12,00%" */
export function percentual(v: Numerico): string {
  return `${numero(v).toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}%`
}

/** ISO "2026-08-30T21:15:00" → "30/08/2026 21:15" */
export function dataHora(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return String(iso)
  return d.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** ISO "2026-08-30T21:15:00" → "21:15" */
export function horaMin(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return String(iso)
  return d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

/** Date local → "2026-08-30" (sem conversão de fuso, ao contrário de toISOString). */
export function dataLocalISO(d: Date = new Date()): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/** Date → "2026-08-30T21:15" (valor aceito por <input type="datetime-local">) */
export function paraInputDateTime(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}

/** Garante os segundos que o LocalDateTime do backend espera. */
export function comSegundos(valorInput: string): string {
  return valorInput.length === 16 ? `${valorInput}:00` : valorInput
}
