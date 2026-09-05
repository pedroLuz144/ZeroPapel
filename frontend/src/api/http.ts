import { auth } from '@/stores/auth'
import type { ApiError } from './types'

const BASE = import.meta.env.VITE_API_BASE ?? ''

/** Erro de resposta HTTP já com a mensagem do backend ({ "error": "..." }) resolvida. */
export class HttpError extends Error {
  constructor(
    readonly status: number,
    readonly corpo: unknown,
  ) {
    super(extrairMensagem(status, corpo))
    this.name = 'HttpError'
  }
}

function extrairMensagem(status: number, corpo: unknown): string {
  if (corpo && typeof corpo === 'object' && 'error' in corpo) {
    return String((corpo as ApiError).error)
  }
  if (status === 0) return 'Erro de conexão com o servidor.'
  return `Erro ${status}`
}

function executar(
  metodo: string,
  caminho: string,
  corpo: unknown,
  token: string | null,
): Promise<Response> {
  return fetch(BASE + caminho, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  })
}

async function renovarSessao(): Promise<boolean> {
  if (!auth.estado.refreshToken) return false
  try {
    const res = await fetch(BASE + '/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: auth.estado.refreshToken }),
    })
    if (!res.ok) return false
    const dados = (await res.json()) as { token: string; refreshToken: string }
    auth.atualizarTokens(dados.token, dados.refreshToken)
    return true
  } catch {
    return false
  }
}

let redirecionando = false

/**
 * Wrapper de fetch: injeta o Bearer token, tenta renovar uma vez em caso de 401
 * e, se ainda assim falhar, limpa a sessão e manda para o /login.
 */
export async function request<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  let res: Response
  try {
    res = await executar(metodo, caminho, corpo, auth.estado.token)
  } catch {
    throw new HttpError(0, null)
  }

  if (res.status === 401 && auth.estado.refreshToken && (await renovarSessao())) {
    res = await executar(metodo, caminho, corpo, auth.estado.token)
  }

  if (res.status === 401) {
    auth.limpar()
    if (!redirecionando) {
      redirecionando = true
      window.location.assign('/login')
    }
    throw new HttpError(401, { error: 'Sessão expirada. Entre novamente.' })
  }

  if (!res.ok) {
    let corpoErro: unknown = null
    try {
      corpoErro = await res.json()
    } catch {
      /* resposta sem corpo JSON */
    }
    throw new HttpError(res.status, corpoErro)
  }

  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

export const http = {
  get: <T>(caminho: string) => request<T>('GET', caminho),
  post: <T>(caminho: string, corpo?: unknown) => request<T>('POST', caminho, corpo),
  patch: <T>(caminho: string, corpo?: unknown) => request<T>('PATCH', caminho, corpo),
  del: <T = void>(caminho: string) => request<T>('DELETE', caminho),
}
