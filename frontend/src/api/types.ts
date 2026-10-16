/*
 * Espelho tipado dos DTOs do backend (com/goldenpetiscaria/zeropapel).
 * Mantém os nomes de campo exatamente como o Jackson serializa.
 */

export type Cargo = 'GERENTE' | 'OPERADOR'
export type StatusItem = 'ATIVO' | 'INATIVO'

export type StatusPedido =
  | 'EM_ABERTO'
  | 'ACEITO'
  | 'EM_PREPARO'
  | 'PRONTO'
  | 'EM_ROTA'
  | 'CONCLUIDO'
  | 'CANCELADO'

// ── Autenticação ──────────────────────────────────────────
export interface LoginRequest {
  usuario: string
  senha: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  cargo: Cargo
}

export interface RefreshRequest {
  refreshToken: string
}

// ── Usuário ───────────────────────────────────────────────
export interface UsuarioResponse {
  id: number
  nome: string
  usuario: string
  cargo: Cargo
  ativo: boolean
}

export interface CadastrarUsuarioRequest {
  nome: string
  usuario: string
  senha: string
  cargo: Cargo
}

export interface AtualizarUsuarioRequest {
  nome?: string
  usuario?: string
  senha?: string
  cargo?: Cargo
}

// ── Categoria ─────────────────────────────────────────────
export interface CategoriaResponse {
  id: number
  nome: string
}

export interface CategoriaRequest {
  nome: string
}

// ── Item ──────────────────────────────────────────────────
export interface ItemResponse {
  id: number
  nome: string
  categoriaId: number
  categoriaNome: string
  preco: number
  status: StatusItem
  descricao: string | null
}

export interface AdicionarItemRequest {
  nome: string
  categoria: number
  preco: number
  descricao?: string | null
}

export interface AtualizarItemRequest {
  nome?: string
  categoria?: number
  preco?: number
  status?: StatusItem
  descricao?: string | null
}

// ── Forma de pagamento ───────────────────────────────────
export interface TaxaResponse {
  id: number
  nome: string
  taxaPercentual: number
}

export interface AdicionarTaxaRequest {
  nome: string
  taxaPercentual: number
}

export interface AtualizarTaxaRequest {
  nome?: string
  taxaPercentual?: number
}

export type FormaDePagamentoResponse = TaxaResponse

// ── Plataforma (tipo de canal; a taxa fica no canal de venda) ─
export interface PlataformaResponse {
  id: number
  nome: string
  /** true = canal de entrega (iFood, AnotaAi); false = balcão. */
  entrega: boolean
}

export interface AdicionarPlataformaRequest {
  nome: string
  entrega: boolean
}

export interface AtualizarPlataformaRequest {
  nome?: string
  entrega?: boolean
}

// ── Loja ──────────────────────────────────────────────────
export interface LojaResponse {
  id: number
  nome: string
  ativo: boolean
}

export interface AdicionarLojaRequest {
  nome: string
}

export interface AtualizarLojaRequest {
  nome?: string
  ativo?: boolean
}

// ── Canal de venda (loja × plataforma) ────────────────────
export type TratamentoTaxaEntrega = 'DESCONTAR' | 'SOMAR' | 'IGNORAR'

export interface CanalDeVendaResponse {
  id: number
  lojaId: number
  lojaNome: string
  plataformaId: number
  plataformaNome: string
  merchantId: string | null
  taxaPercentual: number
  tratamentoTaxaEntrega: TratamentoTaxaEntrega
  ativo: boolean
}

export interface AdicionarCanalDeVendaRequest {
  lojaId: number
  plataformaId: number
  taxaPercentual: number
  merchantId?: string | null
  tratamentoTaxaEntrega?: TratamentoTaxaEntrega
}

export interface AtualizarCanalDeVendaRequest {
  taxaPercentual?: number
  merchantId?: string
  tratamentoTaxaEntrega?: TratamentoTaxaEntrega
  ativo?: boolean
}

// ── Pedido ────────────────────────────────────────────────
export interface ItemPedidoRequest {
  itemId: number
  quantidade: number
}

export interface AdicionarPedidoRequest {
  lojaId: number
  plataformaId: number
  nomeCliente: string | null
  formaDePagamentoId: number
  itens: ItemPedidoRequest[]
}

export interface AtualizarPedidoRequest {
  plataformaId?: number | null
  nomeCliente?: string | null
  formaDePagamentoId?: number | null
  itens?: ItemPedidoRequest[]
}

export interface ItemPedidoResponse {
  itemId: number | null
  itemNome: string
  quantidade: number
  precoUnitario: number
  subtotal: number
}

export interface PedidoResponse {
  id: number
  lojaId: number
  lojaNome: string
  plataformaId: number
  plataformaNome: string
  plataformaPedidoId: string | null
  nomeCliente: string | null
  horarioPedido: string
  formaDePagamentoId: number
  formaDePagamentoNome: string
  status: StatusPedido
  valor: number
  fechado: boolean
  itens: ItemPedidoResponse[]
}

// ── Fechamento de caixa ───────────────────────────────────
export interface RealizarFechamentoRequest {
  de: string
  ate: string
}

export interface ResumoFechamento {
  totalPedidos: number
  faturamentoBruto: number
  totalTaxas: number
  faturamentoLiquido: number
  ticketMedio: number
}

export interface FechamentoPorPlataforma {
  plataforma: string
  qtdPedidos: number
  bruto: number
  taxaPlataforma: number
  taxaPagamento: number
  liquido: number
}

export interface FechamentoPorFormaDePagamento {
  formaDePagamento: string
  qtdPedidos: number
  bruto: number
  taxa: number
  liquido: number
}

export interface ItemRanking {
  itemId: number | null
  itemNome: string
  quantidadeTotal: number
  receitaTotal: number
}

export interface PedidosPorHora {
  hora: number
  qtdPedidos: number
  faturamentoBruto: number
}

export interface FechamentoResponse {
  id: number | null
  de: string
  ate: string
  resumo: ResumoFechamento
  porPlataforma: FechamentoPorPlataforma[]
  porFormaDePagamento: FechamentoPorFormaDePagamento[]
  itensMaisVendidos: ItemRanking[]
  pedidosPorHora: PedidosPorHora[]
}

export interface FechamentoCaixaListagem {
  id: number
  de: string
  ate: string
  geradoEm: string
  geradoPorNome: string
  totalPedidos: number
  faturamentoBruto: number
  totalTaxas: number
  faturamentoLiquido: number
  ticketMedio: number
}

// ── Erro padrão do GlobalExceptionHandler ─────────────────
export interface ApiError {
  error: string
}
