import { http } from './http'
import type {
  AdicionarItemRequest,
  AdicionarPedidoRequest,
  AdicionarPlataformaRequest,
  AdicionarTaxaRequest,
  AtualizarItemRequest,
  AtualizarPedidoRequest,
  AtualizarPlataformaRequest,
  AtualizarTaxaRequest,
  AtualizarUsuarioRequest,
  CadastrarUsuarioRequest,
  CategoriaRequest,
  CategoriaResponse,
  FechamentoCaixaListagem,
  FechamentoResponse,
  FormaDePagamentoResponse,
  ItemResponse,
  LoginRequest,
  LoginResponse,
  PedidoResponse,
  PlataformaResponse,
  RealizarFechamentoRequest,
  StatusPedido,
  UsuarioResponse,
} from './types'

export { HttpError } from './http'

export const authApi = {
  login: (body: LoginRequest) => http.post<LoginResponse>('/auth/login', body),
  logout: (refreshToken: string) => http.post<void>('/auth/logout', { refreshToken }),
}

export const itensApi = {
  listar: () => http.get<ItemResponse[]>('/itens'),
  adicionar: (body: AdicionarItemRequest) => http.post<ItemResponse>('/itens', body),
  atualizar: (id: number, body: AtualizarItemRequest) => http.patch<ItemResponse>(`/itens/${id}`, body),
  excluir: (id: number) => http.del(`/itens/${id}`),
}

export const categoriasApi = {
  listar: () => http.get<CategoriaResponse[]>('/categorias'),
  adicionar: (body: CategoriaRequest) => http.post<CategoriaResponse>('/categorias', body),
  atualizar: (id: number, body: CategoriaRequest) => http.patch<CategoriaResponse>(`/categorias/${id}`, body),
  excluir: (id: number) => http.del(`/categorias/${id}`),
}

export const plataformasApi = {
  listar: () => http.get<PlataformaResponse[]>('/plataformas'),
  adicionar: (body: AdicionarPlataformaRequest) => http.post<PlataformaResponse>('/plataformas', body),
  atualizar: (id: number, body: AtualizarPlataformaRequest) =>
    http.patch<PlataformaResponse>(`/plataformas/${id}`, body),
  excluir: (id: number) => http.del(`/plataformas/${id}`),
}

export const formasPagamentoApi = {
  listar: () => http.get<FormaDePagamentoResponse[]>('/formasDePagamento'),
  adicionar: (body: AdicionarTaxaRequest) => http.post<FormaDePagamentoResponse>('/formasDePagamento', body),
  atualizar: (id: number, body: AtualizarTaxaRequest) =>
    http.patch<FormaDePagamentoResponse>(`/formasDePagamento/${id}`, body),
  excluir: (id: number) => http.del(`/formasDePagamento/${id}`),
}

export const pedidosApi = {
  listar: (de?: string, ate?: string) => {
    const busca = new URLSearchParams()
    if (de) busca.set('de', de)
    if (ate) busca.set('ate', ate)
    const sufixo = busca.toString() ? `?${busca}` : ''
    return http.get<PedidoResponse[]>(`/pedidos${sufixo}`)
  },
  buscar: (id: number) => http.get<PedidoResponse>(`/pedidos/${id}`),
  registrar: (body: AdicionarPedidoRequest) => http.post<PedidoResponse>('/pedidos', body),
  atualizar: (id: number, body: AtualizarPedidoRequest) => http.patch<PedidoResponse>(`/pedidos/${id}`, body),
  atualizarStatus: (id: number, status: StatusPedido) =>
    http.patch<PedidoResponse>(`/pedidos/${id}/status`, { status }),
  excluir: (id: number) => http.del(`/pedidos/${id}`),
}

export const fechamentoApi = {
  listar: () => http.get<FechamentoCaixaListagem[]>('/fechamento'),
  buscar: (id: number) => http.get<FechamentoResponse>(`/fechamento/${id}`),
  realizar: (body: RealizarFechamentoRequest) => http.post<FechamentoResponse>('/fechamento', body),
}

/** Consolidado em tempo real (só-leitura, não persiste). `id` vem null. */
export const dashboardApi = {
  hoje: () => http.get<FechamentoResponse>('/dashboard'),
  periodo: (de: string, ate: string) =>
    http.get<FechamentoResponse>(
      `/dashboard?de=${encodeURIComponent(de)}&ate=${encodeURIComponent(ate)}`,
    ),
}

export const usuariosApi = {
  listar: () => http.get<UsuarioResponse[]>('/usuarios'),
  cadastrar: (body: CadastrarUsuarioRequest) => http.post<void>('/usuarios', body),
  atualizar: (id: number, body: AtualizarUsuarioRequest) => http.patch<UsuarioResponse>(`/usuarios/${id}`, body),
  ativar: (id: number) => http.patch<void>(`/usuarios/${id}/ativar`),
  desativar: (id: number) => http.patch<void>(`/usuarios/${id}/desativar`),
}
