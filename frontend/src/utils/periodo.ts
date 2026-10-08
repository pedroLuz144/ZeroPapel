import type { PedidoResponse, RealizarFechamentoRequest } from '@/api/types'
import { comSegundos, dataLocalISO } from '@/utils/formato'

function doisDigitos(n: number): string {
  return String(n).padStart(2, '0')
}

export function periodoDeHojeAte(instante: Date): RealizarFechamentoRequest {
  const dia = dataLocalISO(instante)
  const horario = [instante.getHours(), instante.getMinutes(), instante.getSeconds()]
    .map(doisDigitos)
    .join(':')
  return { de: `${dia}T00:00:00`, ate: `${dia}T${horario}` }
}

export function periodoDoFechamento(opcoes: {
  aoVivo: boolean
  de: string
  ate: string
  agora: Date
}): RealizarFechamentoRequest {
  if (opcoes.aoVivo) return periodoDeHojeAte(opcoes.agora)
  return { de: comSegundos(opcoes.de), ate: comSegundos(opcoes.ate) }
}

export function pedidosEmAndamentoNoPeriodo(
  pedidos: PedidoResponse[],
  periodo: RealizarFechamentoRequest,
): PedidoResponse[] {
  const inicio = new Date(periodo.de).getTime()
  const fim = new Date(periodo.ate).getTime()
  return pedidos.filter((p) => {
    if (p.status === 'CONCLUIDO' || p.status === 'CANCELADO' || p.fechado) return false
    const horario = new Date(p.horarioPedido).getTime()
    return horario >= inicio && horario <= fim
  })
}
