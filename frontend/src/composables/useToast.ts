import { reactive } from 'vue'

export type TipoToast = 'ok' | 'erro'

export interface Toast {
  id: number
  tipo: TipoToast
  texto: string
}

const fila = reactive<Toast[]>([])
let seq = 0

function fechar(id: number): void {
  const i = fila.findIndex((t) => t.id === id)
  if (i >= 0) fila.splice(i, 1)
}

function mostrar(texto: string, tipo: TipoToast): void {
  const id = ++seq
  fila.push({ id, tipo, texto })
  setTimeout(() => fechar(id), 4200)
}

/** Notificações efêmeras no canto da tela — substitui window.alert(). */
export function useToast() {
  return {
    fila,
    fechar,
    sucesso: (texto: string) => mostrar(texto, 'ok'),
    erro: (texto: string) => mostrar(texto, 'erro'),
  }
}
