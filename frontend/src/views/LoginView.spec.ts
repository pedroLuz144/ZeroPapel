import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import LoginView from '@/views/LoginView.vue'
import { auth } from '@/stores/auth'

const substituirRota = vi.fn()
const irPara = vi.fn()
let consultaDaRota: Record<string, unknown> = {}

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: substituirRota }),
  useRoute: () => ({ query: consultaDaRota }),
}))

vi.mock('@/router', () => ({ default: { replace: vi.fn() } }))

const RESPOSTA_DE_LOGIN = { token: 'acesso', refreshToken: 'refresh', cargo: 'OPERADOR' }

function resposta(status: number, corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), { status })
}

async function tentarEntrar(usuario: string, senha: string) {
  const tela = mount(LoginView)
  await tela.get('input[type="text"]').setValue(usuario)
  await tela.get('input[type="password"]').setValue(senha)
  await tela.get('form').trigger('submit')
  await flushPromises()
  return tela
}

beforeEach(() => {
  auth.limpar()
  consultaDaRota = {}
  substituirRota.mockReset()
  irPara.mockReset()
  vi.stubGlobal('location', { assign: irPara })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('LoginView', () => {
  it('mostra a mensagem do backend e mantém o usuário digitado quando a senha está errada', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => resposta(401, { error: 'Usuário ou senha inválidos' })))

    const tela = await tentarEntrar('ana', 'errada')

    expect(tela.get('.alerta-erro').text()).toBe('Usuário ou senha inválidos')
    expect(tela.get<HTMLInputElement>('input[type="text"]').element.value).toBe('ana')
    expect(tela.get('button').attributes('disabled')).toBeUndefined()
    expect(irPara).not.toHaveBeenCalled()
    expect(substituirRota).not.toHaveBeenCalled()
    expect(auth.autenticado).toBe(false)
  })

  it('avisa quando o servidor está fora do ar', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new TypeError('Failed to fetch')
      }),
    )

    const tela = await tentarEntrar('ana', 'segredo')

    expect(tela.get('.alerta-erro').text()).toBe('Erro de conexão com o servidor.')
    expect(tela.get<HTMLInputElement>('input[type="text"]').element.value).toBe('ana')
    expect(irPara).not.toHaveBeenCalled()
  })

  it('guarda a sessão e segue para o PDV quando o login dá certo', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => resposta(200, { token: 'acesso', refreshToken: 'refresh', cargo: 'OPERADOR' })),
    )

    await tentarEntrar('ana', 'segredo')

    expect(auth.estado).toMatchObject({ token: 'acesso', usuario: 'ana', cargo: 'OPERADOR' })
    expect(substituirRota).toHaveBeenCalledWith({ name: 'pdv' })
  })

  it('volta para a rota de origem quando o redir é um caminho interno', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => resposta(200, RESPOSTA_DE_LOGIN)))
    consultaDaRota = { redir: '/app/financeiro?aba=historico' }

    await tentarEntrar('ana', 'segredo')

    expect(substituirRota).toHaveBeenCalledExactlyOnceWith('/app/financeiro?aba=historico')
  })

  it.each([
    ['URL absoluta', 'https://malicioso.example/app'],
    ['URL relativa ao protocolo', '//malicioso.example/app'],
    ['barra invertida', '/\\malicioso.example'],
    ['caminho sem barra inicial', 'app/pedidos'],
    ['parâmetro repetido', ['/app/pedidos', '/app/financeiro']],
    ['parâmetro vazio', null],
  ])('ignora o redir e segue para o PDV quando ele é %s', async (_caso, redir) => {
    vi.stubGlobal('fetch', vi.fn(async () => resposta(200, RESPOSTA_DE_LOGIN)))
    consultaDaRota = { redir }

    await tentarEntrar('ana', 'segredo')

    expect(substituirRota).toHaveBeenCalledExactlyOnceWith({ name: 'pdv' })
  })
})
