import { describe, expect, it } from 'vitest'
import type { PedidoResponse, StatusPedido } from '@/api/types'
import { pedidosEmAndamentoNoPeriodo, periodoDeHojeAte, periodoDoFechamento } from '@/utils/periodo'

function pedido(id: number, status: StatusPedido, horarioPedido: string, fechado = false): PedidoResponse {
  return {
    id,
    lojaId: 1,
    lojaNome: 'Matriz',
    plataformaId: 1,
    plataformaNome: 'Balcão',
    nomeCliente: null,
    horarioPedido,
    formaDePagamentoId: 1,
    formaDePagamentoNome: 'Pix',
    status,
    valor: 10,
    fechado,
    itens: [],
  }
}

describe('periodoDeHojeAte', () => {
  it('vai da meia-noite local até o instante informado, com segundos', () => {
    expect(periodoDeHojeAte(new Date(2026, 7, 30, 23, 30, 45))).toEqual({
      de: '2026-08-30T00:00:00',
      ate: '2026-08-30T23:30:45',
    })
  })

  it('completa com zeros a hora, o minuto e o segundo de um dígito', () => {
    expect(periodoDeHojeAte(new Date(2026, 0, 5, 7, 4, 9))).toEqual({
      de: '2026-01-05T00:00:00',
      ate: '2026-01-05T07:04:09',
    })
  })
})

describe('periodoDoFechamento', () => {
  const preenchidoNaAbertura = { de: '2026-08-30T00:00', ate: '2026-08-30T18:00' }

  it('ao vivo, ignora o "até" de quando a tela abriu e fecha até agora', () => {
    const periodo = periodoDoFechamento({
      aoVivo: true,
      ...preenchidoNaAbertura,
      agora: new Date(2026, 7, 30, 23, 30, 45),
    })

    expect(periodo).toEqual({ de: '2026-08-30T00:00:00', ate: '2026-08-30T23:30:45' })
  })

  it('ao vivo depois da meia-noite, usa o dia do instante atual', () => {
    const periodo = periodoDoFechamento({
      aoVivo: true,
      ...preenchidoNaAbertura,
      agora: new Date(2026, 7, 31, 0, 20, 0),
    })

    expect(periodo).toEqual({ de: '2026-08-31T00:00:00', ate: '2026-08-31T00:20:00' })
  })

  it('com período escolhido pelo usuário, respeita o que ele escolheu', () => {
    const periodo = periodoDoFechamento({
      aoVivo: false,
      de: '2026-08-29T18:00',
      ate: '2026-08-30T02:00',
      agora: new Date(2026, 7, 30, 23, 30, 45),
    })

    expect(periodo).toEqual({ de: '2026-08-29T18:00:00', ate: '2026-08-30T02:00:00' })
  })
})

describe('pedidosEmAndamentoNoPeriodo', () => {
  const periodo = { de: '2026-08-30T00:00:00', ate: '2026-08-30T23:30:45' }

  it('conta só o que ainda não terminou e está dentro do período', () => {
    const pedidos = [
      pedido(1, 'EM_PREPARO', '2026-08-30T21:15:00'),
      pedido(2, 'CONCLUIDO', '2026-08-30T21:20:00'),
      pedido(3, 'CANCELADO', '2026-08-30T21:25:00'),
      pedido(4, 'EM_ABERTO', '2026-08-29T23:50:00'),
      pedido(5, 'PRONTO', '2026-08-30T23:31:00'),
      pedido(6, 'EM_ROTA', '2026-08-30T23:30:45'),
    ]

    expect(pedidosEmAndamentoNoPeriodo(pedidos, periodo).map((p) => p.id)).toEqual([1, 6])
  })

  it('deixa de fora o pedido que um fechamento anterior já travou', () => {
    const pedidos = [pedido(1, 'EM_PREPARO', '2026-08-30T21:15:00', true)]

    expect(pedidosEmAndamentoNoPeriodo(pedidos, periodo)).toEqual([])
  })

  it('compara pelo instante mesmo quando o horário vem com fração de segundo', () => {
    const pedidos = [
      pedido(1, 'ACEITO', '2026-08-30T23:30:45.900'),
      pedido(2, 'ACEITO', '2026-08-30T00:00:00.120'),
    ]

    expect(pedidosEmAndamentoNoPeriodo(pedidos, periodo).map((p) => p.id)).toEqual([2])
  })
})
