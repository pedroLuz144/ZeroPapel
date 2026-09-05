import { describe, expect, it } from 'vitest'
import { comSegundos, dataLocalISO, moeda, paraInputDateTime, percentual } from './formato'

/**
 * O Intl separa o símbolo do valor com espaço não-quebrável (U+00A0/U+202F),
 * que não é o espaço comum digitado no teste. Normaliza antes de comparar.
 */
const semNbsp = (s: string) => s.replace(/[  ]/g, ' ')

describe('moeda', () => {
  it('formata número como real brasileiro', () => {
    expect(semNbsp(moeda(18.5))).toBe('R$ 18,50')
  })

  it('trata null, undefined e texto não numérico como zero', () => {
    expect(semNbsp(moeda(null))).toBe('R$ 0,00')
    expect(semNbsp(moeda(undefined))).toBe('R$ 0,00')
    expect(semNbsp(moeda('abc'))).toBe('R$ 0,00')
  })
})

describe('percentual', () => {
  it('usa sempre duas casas decimais', () => {
    expect(percentual(12)).toBe('12,00%')
    expect(percentual('7.5')).toBe('7,50%')
  })
})

describe('dataLocalISO', () => {
  it('usa a data local, sem o deslocamento de fuso do toISOString', () => {
    // 21h em UTC-3 vira o dia seguinte no toISOString; aqui deve continuar dia 30.
    expect(dataLocalISO(new Date(2026, 7, 30, 21, 15))).toBe('2026-08-30')
  })
})

describe('paraInputDateTime', () => {
  it('gera o formato aceito por <input type="datetime-local">', () => {
    expect(paraInputDateTime(new Date(2026, 7, 30, 21, 15))).toBe('2026-08-30T21:15')
  })
})

describe('comSegundos', () => {
  it('completa os segundos que o LocalDateTime do backend espera', () => {
    expect(comSegundos('2026-08-30T21:15')).toBe('2026-08-30T21:15:00')
  })

  it('mantém o valor quando os segundos já vieram', () => {
    expect(comSegundos('2026-08-30T21:15:42')).toBe('2026-08-30T21:15:42')
  })
})
