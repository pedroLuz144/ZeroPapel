import { auth } from '@/stores/auth'
import router from '@/router'
import type { LoginResponse } from '@/api/types'

const BASE = import.meta.env.VITE_API_BASE ?? ''

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
  if (corpo && typeof corpo === 'object') {
    if ('error' in corpo) return String(corpo.error)
    const mensagensDeValidacao = Object.values(corpo).filter(
      (valor): valor is string => typeof valor === 'string',
    )
    if (mensagensDeValidacao.length > 0) return mensagensDeValidacao.join('; ')
  }
  if (status === 0) return 'Erro de conexão com o servidor.'
  return `Erro ${status}`
}

async function executar(
  metodo: string,
  caminho: string,
  corpo: unknown,
  token: string | null,
): Promise<Response> {
  try {
    return await fetch(BASE + caminho, {
      method: metodo,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    })
  } catch {
    throw new HttpError(0, null)
  }
}

let renovacaoEmAndamento: Promise<boolean> | null = null

function renovarSessao(): Promise<boolean> {
  renovacaoEmAndamento ??= trocarRefreshToken().finally(() => {
    renovacaoEmAndamento = null
  })
  return renovacaoEmAndamento
}

async function trocarRefreshToken(): Promise<boolean> {
  if (!auth.estado.refreshToken) return false
  try {
    const res = await fetch(BASE + '/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: auth.estado.refreshToken }),
    })
    if (!res.ok) return false
    const dados = (await res.json()) as LoginResponse
    auth.atualizarTokens(dados.token, dados.refreshToken, dados.cargo)
    return true
  } catch {
    return false
  }
}

let redirecionando = false

function levarAoLogin(): void {
  if (redirecionando) return
  redirecionando = true
  const origem = router.currentRoute.value
  const destino = origem.meta.publico
    ? { name: 'login' }
    : { name: 'login', query: { redir: origem.fullPath } }
  router.replace(destino).finally(() => {
    redirecionando = false
  })
}

const PREFIXO_DAS_ROTAS_DE_AUTENTICACAO = '/auth/'

export async function request<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  const dependeDeSessao = !caminho.startsWith(PREFIXO_DAS_ROTAS_DE_AUTENTICACAO)
  let res = await executar(metodo, caminho, corpo, auth.estado.token)

  if (dependeDeSessao && res.status === 401) {
    if (await renovarSessao()) {
      res = await executar(metodo, caminho, corpo, auth.estado.token)
    }
    if (res.status === 401) {
      auth.limpar()
      levarAoLogin()
      throw new HttpError(401, { error: 'Sessão expirada. Entre novamente.' })
    }
  }

  if (!res.ok) {
    let corpoErro: unknown = null
    try {
      corpoErro = await res.json()
    } catch {
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
