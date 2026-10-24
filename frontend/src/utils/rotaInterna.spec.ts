import { describe, expect, it } from 'vitest'
import { ehCaminhoInterno } from '@/utils/rotaInterna'

describe('ehCaminhoInterno', () => {
  it.each(['/app/pedidos', '/app/financeiro?aba=historico', '/'])('aceita o caminho interno %s', (caminho) => {
    expect(ehCaminhoInterno(caminho)).toBe(true)
  })

  it.each([
    'https://malicioso.example',
    '//malicioso.example',
    '/\\malicioso.example',
    'javascript:alert(1)',
    'app/pedidos',
    '',
  ])('recusa %s', (valor) => {
    expect(ehCaminhoInterno(valor)).toBe(false)
  })

  it('recusa o que não é texto', () => {
    expect(ehCaminhoInterno(null)).toBe(false)
    expect(ehCaminhoInterno(undefined)).toBe(false)
    expect(ehCaminhoInterno(['/app/pedidos'])).toBe(false)
  })
})
