import { reactive } from 'vue'
import type { Cargo } from '@/api/types'

interface EstadoAuth {
  token: string | null
  refreshToken: string | null
  usuario: string | null
  cargo: Cargo | null
}

const CHAVE = 'zeropapel.auth'

function carregar(): EstadoAuth {
  try {
    const cru = localStorage.getItem(CHAVE)
    if (cru) return { ...vazio(), ...JSON.parse(cru) }
  } catch {
    /* storage indisponível ou corrompido — começa deslogado */
  }
  return vazio()
}

function vazio(): EstadoAuth {
  return { token: null, refreshToken: null, usuario: null, cargo: null }
}

const estado = reactive<EstadoAuth>(carregar())

function persistir(): void {
  try {
    localStorage.setItem(CHAVE, JSON.stringify(estado))
  } catch {
    /* ignora falha de persistência */
  }
}

export const auth = {
  estado,

  get autenticado(): boolean {
    return !!estado.token
  },

  get isGerente(): boolean {
    return estado.cargo === 'GERENTE'
  },

  definirSessao(dados: {
    token: string
    refreshToken: string
    usuario: string
    cargo: Cargo
  }): void {
    Object.assign(estado, dados)
    persistir()
  },

  atualizarTokens(token: string, refreshToken: string): void {
    estado.token = token
    estado.refreshToken = refreshToken
    persistir()
  },

  limpar(): void {
    Object.assign(estado, vazio())
    try {
      localStorage.removeItem(CHAVE)
    } catch {
      /* ignora */
    }
  },
}
