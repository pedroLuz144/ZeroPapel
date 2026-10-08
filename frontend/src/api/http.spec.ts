import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const SESSAO = { token: 'acesso-antigo', refreshToken: 'refresh-antigo', usuario: 'ana', cargo: 'GERENTE' }

const irPara = vi.fn()

const roteador = vi.hoisted(() => ({
  replace: vi.fn(),
  currentRoute: { value: { fullPath: '/app/pedidos', meta: { publico: false } } },
}))

vi.mock('@/router', () => ({ default: roteador }))

function resposta(status: number, corpo?: unknown): Response {
  return new Response(corpo === undefined ? null : JSON.stringify(corpo), { status })
}

function caminhosChamados(buscar: ReturnType<typeof vi.fn>): string[] {
  return buscar.mock.calls.map((chamada) => String(chamada[0]))
}

async function carregarModulos(sessao?: typeof SESSAO) {
  if (sessao) localStorage.setItem('zeropapel.auth', JSON.stringify(sessao))
  vi.resetModules()
  const { auth } = await import('@/stores/auth')
  const { http } = await import('@/api/http')
  return { auth, http }
}

function simularBackend(
  responder: (caminho: string, autorizacao: string | null) => Response | Promise<Response>,
) {
  const buscar = vi.fn(async (caminho: string, opcoes?: RequestInit) =>
    responder(caminho, new Headers(opcoes?.headers).get('Authorization')),
  )
  vi.stubGlobal('fetch', buscar)
  return buscar
}

beforeEach(() => {
  localStorage.clear()
  irPara.mockReset()
  roteador.replace.mockReset()
  roteador.replace.mockResolvedValue(undefined)
  roteador.currentRoute.value = { fullPath: '/app/pedidos', meta: { publico: false } }
  vi.stubGlobal('location', { assign: irPara })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('rotas de autenticação', () => {
  it('401 no login propaga a mensagem do backend sem redirecionar nem tentar refresh', async () => {
    const buscar = simularBackend(() => resposta(401, { error: 'Usuário ou senha inválidos' }))
    const { http } = await carregarModulos()

    await expect(http.post('/auth/login', { usuario: 'ana', senha: 'errada' })).rejects.toMatchObject({
      name: 'HttpError',
      status: 401,
      message: 'Usuário ou senha inválidos',
    })

    expect(caminhosChamados(buscar)).toEqual(['/auth/login'])
    expect(irPara).not.toHaveBeenCalled()
    expect(roteador.replace).not.toHaveBeenCalled()
  })

  it('401 no login não tenta refresh nem apaga uma sessão que ainda esteja guardada', async () => {
    const buscar = simularBackend(() => resposta(401, { error: 'Usuário ou senha inválidos' }))
    const { auth, http } = await carregarModulos(SESSAO)

    await expect(http.post('/auth/login', { usuario: 'ana', senha: 'errada' })).rejects.toMatchObject({
      status: 401,
      message: 'Usuário ou senha inválidos',
    })

    expect(caminhosChamados(buscar)).toEqual(['/auth/login'])
    expect(auth.estado.refreshToken).toBe('refresh-antigo')
    expect(irPara).not.toHaveBeenCalled()
    expect(roteador.replace).not.toHaveBeenCalled()
  })

  it('401 no logout é devolvido a quem chamou, sem refresh e sem redirecionar', async () => {
    const buscar = simularBackend(() => resposta(401, { error: 'Refresh token inválido' }))
    const { http } = await carregarModulos(SESSAO)

    await expect(http.post('/auth/logout', { refreshToken: 'refresh-antigo' })).rejects.toMatchObject({
      status: 401,
      message: 'Refresh token inválido',
    })

    expect(caminhosChamados(buscar)).toEqual(['/auth/logout'])
    expect(irPara).not.toHaveBeenCalled()
    expect(roteador.replace).not.toHaveBeenCalled()
  })
})

describe('rotas que dependem de sessão', () => {
  it('401 com refresh inválido limpa a sessão e leva ao login guardando a rota de origem', async () => {
    roteador.currentRoute.value = { fullPath: '/app/financeiro?aba=historico', meta: { publico: false } }
    const buscar = simularBackend((caminho) =>
      caminho === '/auth/refresh'
        ? resposta(401, { error: 'Refresh token expirado, faça login novamente' })
        : resposta(401),
    )
    const { auth, http } = await carregarModulos(SESSAO)

    await expect(http.get('/itens')).rejects.toMatchObject({
      status: 401,
      message: 'Sessão expirada. Entre novamente.',
    })

    expect(caminhosChamados(buscar)).toEqual(['/itens', '/auth/refresh'])
    expect(auth.autenticado).toBe(false)
    expect(auth.estado.refreshToken).toBeNull()
    expect(roteador.replace).toHaveBeenCalledExactlyOnceWith({
      name: 'login',
      query: { redir: '/app/financeiro?aba=historico' },
    })
    expect(irPara).not.toHaveBeenCalled()
  })

  it('401 sem refresh token guardado leva ao login sem chamar o refresh', async () => {
    const buscar = simularBackend(() => resposta(401))
    const { http } = await carregarModulos({ ...SESSAO, refreshToken: '' })

    await expect(http.get('/itens')).rejects.toMatchObject({ status: 401 })

    expect(caminhosChamados(buscar)).toEqual(['/itens'])
    expect(roteador.replace).toHaveBeenCalledExactlyOnceWith({
      name: 'login',
      query: { redir: '/app/pedidos' },
    })
  })

  it('sessão expirada numa rota pública leva ao login sem redir', async () => {
    simularBackend(() => resposta(401))
    roteador.currentRoute.value = { fullPath: '/login', meta: { publico: true } }
    const { http } = await carregarModulos({ ...SESSAO, refreshToken: '' })

    await expect(http.get('/itens')).rejects.toMatchObject({ status: 401 })

    expect(roteador.replace).toHaveBeenCalledExactlyOnceWith({ name: 'login' })
  })

  it('chamadas simultâneas com sessão expirada redirecionam uma vez só', async () => {
    simularBackend(() => resposta(401))
    const { http } = await carregarModulos({ ...SESSAO, refreshToken: '' })

    await Promise.allSettled([http.get('/itens'), http.get('/pedidos'), http.get('/plataformas')])

    expect(roteador.replace).toHaveBeenCalledTimes(1)
  })

  it('uma segunda expiração, depois de o redirecionamento terminar, redireciona de novo', async () => {
    simularBackend(() => resposta(401))
    const { http } = await carregarModulos({ ...SESSAO, refreshToken: '' })

    await expect(http.get('/itens')).rejects.toMatchObject({ status: 401 })
    await Promise.resolve()
    await Promise.resolve()
    await expect(http.get('/pedidos')).rejects.toMatchObject({ status: 401 })

    expect(roteador.replace).toHaveBeenCalledTimes(2)
  })

  it('401 com refresh válido renova os tokens e repete a chamada', async () => {
    const buscar = simularBackend((caminho, autorizacao) => {
      if (caminho === '/auth/refresh') {
        return resposta(200, { token: 'acesso-novo', refreshToken: 'refresh-novo', cargo: 'GERENTE' })
      }
      return autorizacao === 'Bearer acesso-novo' ? resposta(200, [{ id: 1 }]) : resposta(401)
    })
    const { auth, http } = await carregarModulos(SESSAO)

    await expect(http.get('/itens')).resolves.toEqual([{ id: 1 }])

    expect(caminhosChamados(buscar)).toEqual(['/itens', '/auth/refresh', '/itens'])
    expect(auth.estado.token).toBe('acesso-novo')
    expect(auth.estado.refreshToken).toBe('refresh-novo')
    expect(irPara).not.toHaveBeenCalled()
    expect(roteador.replace).not.toHaveBeenCalled()
  })

  it('o refresh grava o cargo devolvido pelo backend, na memória e no storage', async () => {
    simularBackend((caminho, autorizacao) => {
      if (caminho === '/auth/refresh') {
        return resposta(200, { token: 'acesso-novo', refreshToken: 'refresh-novo', cargo: 'OPERADOR' })
      }
      return autorizacao === 'Bearer acesso-novo' ? resposta(200, []) : resposta(401)
    })
    const { auth, http } = await carregarModulos(SESSAO)
    expect(auth.isGerente).toBe(true)

    await http.get('/itens')

    expect(auth.estado.cargo).toBe('OPERADOR')
    expect(auth.isGerente).toBe(false)
    expect(auth.estado.usuario).toBe('ana')
    expect(JSON.parse(localStorage.getItem('zeropapel.auth') ?? '{}')).toMatchObject({
      token: 'acesso-novo',
      cargo: 'OPERADOR',
    })
  })

  it('chamadas simultâneas com 401 compartilham um único refresh e nenhuma perde a sessão', async () => {
    let refreshJaUsado = false
    const buscar = simularBackend((caminho, autorizacao) => {
      if (caminho === '/auth/refresh') {
        if (refreshJaUsado) return resposta(401, { error: 'Refresh token inválido' })
        refreshJaUsado = true
        return resposta(200, { token: 'acesso-novo', refreshToken: 'refresh-novo', cargo: 'GERENTE' })
      }
      return autorizacao === 'Bearer acesso-novo' ? resposta(200, { caminho }) : resposta(401)
    })
    const { auth, http } = await carregarModulos(SESSAO)

    await expect(
      Promise.all([http.get('/itens'), http.get('/pedidos'), http.get('/plataformas')]),
    ).resolves.toEqual([{ caminho: '/itens' }, { caminho: '/pedidos' }, { caminho: '/plataformas' }])

    expect(caminhosChamados(buscar).filter((caminho) => caminho === '/auth/refresh')).toHaveLength(1)
    expect(auth.estado.token).toBe('acesso-novo')
    expect(irPara).not.toHaveBeenCalled()
    expect(roteador.replace).not.toHaveBeenCalled()
  })

  it('403 mostra a mensagem do backend e mantém a sessão', async () => {
    simularBackend(() => resposta(403, { error: 'Acesso negado' }))
    const { auth, http } = await carregarModulos(SESSAO)

    await expect(http.del('/pedidos/7')).rejects.toMatchObject({ status: 403, message: 'Acesso negado' })

    expect(auth.autenticado).toBe(true)
    expect(irPara).not.toHaveBeenCalled()
    expect(roteador.replace).not.toHaveBeenCalled()
  })
})

describe('mensagem de erro', () => {
  it('falha de rede vira HttpError de status 0, inclusive na repetição após o refresh', async () => {
    let chamadasAoRecurso = 0
    simularBackend((caminho) => {
      if (caminho === '/auth/refresh') {
        return resposta(200, { token: 'acesso-novo', refreshToken: 'refresh-novo', cargo: 'GERENTE' })
      }
      chamadasAoRecurso += 1
      if (chamadasAoRecurso === 1) return resposta(401)
      throw new TypeError('Failed to fetch')
    })
    const { http } = await carregarModulos(SESSAO)

    await expect(http.get('/itens')).rejects.toMatchObject({
      name: 'HttpError',
      status: 0,
      message: 'Erro de conexão com o servidor.',
    })
    expect(irPara).not.toHaveBeenCalled()
    expect(roteador.replace).not.toHaveBeenCalled()
  })

  it('400 de validação mostra as mensagens dos campos em vez de "Erro 400"', async () => {
    simularBackend(() => resposta(400, { senha: 'Senha deve ter no mínimo 4 caracteres' }))
    const { http } = await carregarModulos(SESSAO)

    await expect(http.post('/usuarios', {})).rejects.toMatchObject({
      status: 400,
      message: 'Senha deve ter no mínimo 4 caracteres',
    })
  })

  it('resposta de erro sem corpo cai no texto genérico com o status', async () => {
    simularBackend(() => resposta(500))
    const { http } = await carregarModulos(SESSAO)

    await expect(http.get('/itens')).rejects.toMatchObject({ status: 500, message: 'Erro 500' })
  })
})
