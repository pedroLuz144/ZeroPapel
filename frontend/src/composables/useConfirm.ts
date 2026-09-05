import { reactive } from 'vue'

interface EstadoConfirm {
  aberto: boolean
  titulo: string
  mensagem: string
  rotuloConfirmar: string
  resolver: ((ok: boolean) => void) | null
}

const estado = reactive<EstadoConfirm>({
  aberto: false,
  titulo: 'Confirmar',
  mensagem: '',
  rotuloConfirmar: 'Confirmar',
  resolver: null,
})

function responder(ok: boolean): void {
  estado.aberto = false
  estado.resolver?.(ok)
  estado.resolver = null
}

/** Diálogo de confirmação assíncrono — substitui window.confirm(). */
export function useConfirm() {
  function confirmar(
    mensagem: string,
    opcoes: { titulo?: string; rotuloConfirmar?: string } = {},
  ): Promise<boolean> {
    estado.mensagem = mensagem
    estado.titulo = opcoes.titulo ?? 'Confirmar'
    estado.rotuloConfirmar = opcoes.rotuloConfirmar ?? 'Confirmar'
    estado.aberto = true
    return new Promise<boolean>((resolve) => {
      estado.resolver = resolve
    })
  }

  return { estado, confirmar, responder }
}
