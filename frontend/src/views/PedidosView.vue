<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseField from '@/components/BaseField.vue'
import BaseModal from '@/components/BaseModal.vue'
import CarrinhoEditor from '@/components/CarrinhoEditor.vue'
import { formasPagamentoApi, itensApi, pedidosApi, plataformasApi } from '@/api'
import type {
  ItemResponse,
  PedidoResponse,
  PlataformaResponse,
  StatusPedido,
  TaxaResponse,
} from '@/api/types'
import type { LinhaCarrinho } from '@/types'
import { dataLocalISO, horaMin, moeda } from '@/utils/formato'
import { auth } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

const toast = useToast()
const { confirmar } = useConfirm()

// ── Etapas ────────────────────────────────────────────────
const STATUS_ATIVOS: StatusPedido[] = [
  'EM_ABERTO',
  'ACEITO',
  'EM_PREPARO',
  'PRONTO',
  'EM_ROTA',
  'CONCLUIDO',
]
const STATUS_TODOS: StatusPedido[] = [...STATUS_ATIVOS, 'CANCELADO']
const STATUS_LABEL: Record<StatusPedido, string> = {
  EM_ABERTO: 'Em aberto',
  ACEITO: 'Aceito',
  EM_PREPARO: 'Em preparo',
  PRONTO: 'Pronto',
  EM_ROTA: 'Em rota',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
}

function proximo(s: StatusPedido, ehEntrega: boolean): StatusPedido | null {
  const i = STATUS_ATIVOS.indexOf(s)
  if (i < 0 || i >= STATUS_ATIVOS.length - 1) return null
  const prox = STATUS_ATIVOS[i + 1]
  // Balcão não tem "Em rota": pula direto para Concluído.
  return prox === 'EM_ROTA' && !ehEntrega ? 'CONCLUIDO' : prox
}

function corDot(s: StatusPedido): string {
  if (s === 'EM_ABERTO') return 'var(--cor-acento)'
  if (s === 'PRONTO') return 'var(--cor-ok)'
  if (s === 'CANCELADO') return 'var(--cor-erro)'
  return 'var(--cor-texto-fraco)'
}

// ── Estado ────────────────────────────────────────────────
const pedidos = ref<PedidoResponse[]>([])
const plataformas = ref<PlataformaResponse[]>([])
const formasPagamento = ref<TaxaResponse[]>([])
const itens = ref<ItemResponse[]>([])

/** Um pedido é "de entrega" se a plataforma dele tem o flag (default: sim, quando desconhecida). */
function ehEntrega(p: PedidoResponse): boolean {
  return plataformas.value.find((pl) => pl.id === p.plataformaId)?.entrega ?? true
}

const carregando = ref(true)
const erro = ref('')
const mostrarCancelados = ref(false)

const ultimaAtualizacao = ref(Date.now())
const agora = ref(Date.now())
let timer: number | undefined

const detalhe = ref<PedidoResponse | null>(null)

const editando = ref<PedidoResponse | null>(null)
const formPlataformaId = ref<number | null>(null)
const formFormaId = ref<number | null>(null)
const formNomeCliente = ref('')
const carrinhoEdicao = ref<LinhaCarrinho[]>([])
const erroEdicao = ref('')
const salvando = ref(false)

// ── Derivados ─────────────────────────────────────────────
function ehDeHoje(p: PedidoResponse): boolean {
  return p.horarioPedido.slice(0, 10) === dataLocalISO()
}

const pedidosDeHoje = computed(() =>
  pedidos.value.filter((p) => ehDeHoje(p) && p.status !== 'CANCELADO'),
)

const metricasHoje = computed(() => {
  const ps = pedidosDeHoje.value
  const total = ps.reduce((s, p) => s + Number(p.valor), 0)
  return { qtd: ps.length, total, ticket: ps.length ? total / ps.length : 0 }
})

const canceladosHoje = computed(
  () => pedidos.value.filter((p) => p.status === 'CANCELADO' && ehDeHoje(p)).length,
)

function pedidosDaColuna(status: StatusPedido): PedidoResponse[] {
  const soHoje = status === 'CONCLUIDO' || status === 'CANCELADO'
  return pedidos.value
    .filter((p) => p.status === status && (!soHoje || ehDeHoje(p)))
    .sort((a, b) => b.horarioPedido.localeCompare(a.horarioPedido))
}

const colunas = computed(() => {
  const lista = mostrarCancelados.value ? STATUS_TODOS : STATUS_ATIVOS
  return lista.map((status) => ({ status, label: STATUS_LABEL[status], itens: pedidosDaColuna(status) }))
})

const desdeAtualizacao = computed(() => {
  const s = Math.max(0, Math.round((agora.value - ultimaAtualizacao.value) / 1000))
  if (s < 60) return `há ${s}s`
  return `há ${Math.round(s / 60)} min`
})

function resumoItens(p: PedidoResponse): string {
  return p.itens.map((it) => `${it.quantidade}× ${it.itemNome}`).join('  ·  ')
}

// ── Carga ─────────────────────────────────────────────────
async function carregar(): Promise<void> {
  erro.value = ''
  try {
    pedidos.value = await pedidosApi.listar()
    ultimaAtualizacao.value = Date.now()
    agora.value = Date.now()
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar os pedidos.'
  }
}

onMounted(async () => {
  try {
    const [ped, pla, fpg, itn] = await Promise.all([
      pedidosApi.listar(),
      plataformasApi.listar(),
      formasPagamentoApi.listar(),
      itensApi.listar(),
    ])
    pedidos.value = ped
    plataformas.value = pla
    formasPagamento.value = fpg
    itens.value = itn
    ultimaAtualizacao.value = Date.now()
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar os dados.'
  } finally {
    carregando.value = false
  }
  timer = window.setInterval(() => (agora.value = Date.now()), 10000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// ── Status ────────────────────────────────────────────────
async function mudarStatus(p: PedidoResponse, novo: StatusPedido): Promise<void> {
  try {
    const atualizado = await pedidosApi.atualizarStatus(p.id, novo)
    const i = pedidos.value.findIndex((x) => x.id === p.id)
    if (i >= 0) pedidos.value[i] = atualizado
    if (detalhe.value?.id === p.id) detalhe.value = atualizado
    toast.sucesso(`Pedido #${p.id} → ${STATUS_LABEL[novo]}`)
  } catch (e) {
    toast.erro(e instanceof Error ? e.message : 'Erro ao mudar o status.')
  }
}

async function avancar(p: PedidoResponse): Promise<void> {
  const prox = proximo(p.status, ehEntrega(p))
  if (prox) await mudarStatus(p, prox)
}

async function aoEscolherStatus(p: PedidoResponse, evento: Event): Promise<void> {
  const alvo = evento.target as HTMLSelectElement
  const novo = alvo.value as StatusPedido
  alvo.value = p.status // desfaz visualmente; a fonte da verdade é o dado
  if (novo === p.status) return
  if (novo === 'CANCELADO') {
    const ok = await confirmar(`Cancelar o pedido #${p.id}?`, {
      titulo: 'Cancelar pedido',
      rotuloConfirmar: 'Cancelar pedido',
    })
    if (!ok) return
  }
  await mudarStatus(p, novo)
}

// ── Detalhe ───────────────────────────────────────────────
function abrirDetalhe(p: PedidoResponse): void {
  detalhe.value = p
}

function editarDoDetalhe(): void {
  const p = detalhe.value
  detalhe.value = null
  if (p) abrirEdicao(p)
}

async function excluirDoDetalhe(): Promise<void> {
  const p = detalhe.value
  detalhe.value = null
  if (p) await excluir(p)
}

// ── Edição ────────────────────────────────────────────────
function abrirEdicao(p: PedidoResponse): void {
  editando.value = p
  formPlataformaId.value = p.plataformaId
  formFormaId.value = p.formaDePagamentoId
  formNomeCliente.value = p.nomeCliente ?? ''
  carrinhoEdicao.value = p.itens.map((it) => ({
    itemId: it.itemId,
    nome: it.itemNome,
    preco: Number(it.precoUnitario),
    quantidade: it.quantidade,
  }))
  erroEdicao.value = ''
}

const totalEdicao = computed(() =>
  carrinhoEdicao.value.reduce((s, l) => s + l.preco * l.quantidade, 0),
)

async function salvarEdicao(): Promise<void> {
  if (!editando.value) return
  if (formPlataformaId.value == null || formFormaId.value == null) {
    erroEdicao.value = 'Selecione a plataforma e a forma de pagamento.'
    return
  }
  if (carrinhoEdicao.value.length === 0) {
    erroEdicao.value = 'O pedido precisa ter pelo menos um item.'
    return
  }

  salvando.value = true
  erroEdicao.value = ''
  try {
    const atualizado = await pedidosApi.atualizar(editando.value.id, {
      plataformaId: formPlataformaId.value,
      nomeCliente: formNomeCliente.value.trim() || null,
      formaDePagamentoId: formFormaId.value,
      itens: carrinhoEdicao.value.map((l) => ({ itemId: l.itemId, quantidade: l.quantidade })),
    })
    const i = pedidos.value.findIndex((x) => x.id === atualizado.id)
    if (i >= 0) pedidos.value[i] = atualizado
    toast.sucesso(`Pedido #${editando.value.id} atualizado.`)
    editando.value = null
  } catch (e) {
    erroEdicao.value = e instanceof Error ? e.message : 'Erro ao salvar o pedido.'
  } finally {
    salvando.value = false
  }
}

async function excluir(p: PedidoResponse): Promise<void> {
  const ok = await confirmar(`Excluir o pedido #${p.id}? Esta ação não pode ser desfeita.`, {
    titulo: 'Excluir pedido',
    rotuloConfirmar: 'Excluir',
  })
  if (!ok) return
  try {
    await pedidosApi.excluir(p.id)
    pedidos.value = pedidos.value.filter((x) => x.id !== p.id)
    toast.sucesso(`Pedido #${p.id} excluído.`)
  } catch (e) {
    toast.erro(e instanceof Error ? e.message : 'Erro ao excluir o pedido.')
  }
}
</script>

<template>
  <div v-if="carregando" class="estado-tela">Carregando…</div>

  <template v-else>
    <div class="barra-status">
      <span class="barra-status__estado">
        <span class="ponto" aria-hidden="true"></span>
        Sistema online · atualizado {{ desdeAtualizacao }}
      </span>
      <span class="barra-status__acoes">
        <button
          v-if="canceladosHoje > 0"
          class="link-acao"
          @click="mostrarCancelados = !mostrarCancelados"
        >
          {{ mostrarCancelados ? 'Ocultar' : 'Ver' }} cancelados ({{ canceladosHoje }})
        </button>
        <button class="link-acao" @click="carregar">Atualizar agora</button>
      </span>
    </div>

    <div v-if="erro" class="alerta-erro">{{ erro }}</div>

    <div class="metricas">
      <div class="metrica">
        <div class="metrica__rotulo">Pedidos hoje</div>
        <div class="metrica__valor">{{ metricasHoje.qtd }}</div>
      </div>
      <div class="metrica">
        <div class="metrica__rotulo">Faturamento do dia</div>
        <div class="metrica__valor">{{ moeda(metricasHoje.total) }}</div>
      </div>
      <div class="metrica">
        <div class="metrica__rotulo">Ticket médio</div>
        <div class="metrica__valor">{{ moeda(metricasHoje.ticket) }}</div>
      </div>
    </div>

    <div class="quadro">
      <section v-for="col in colunas" :key="col.status" class="coluna">
        <header class="coluna__cabecalho">
          <span class="coluna__titulo">
            <span class="coluna__dot" :style="{ background: corDot(col.status) }"></span>
            {{ col.label }}
          </span>
          <span class="coluna__contador">{{ col.itens.length }}</span>
        </header>

        <div class="coluna__lista">
          <p v-if="col.itens.length === 0" class="coluna__vazio">—</p>

          <article v-for="p in col.itens" :key="p.id" class="pedido-card">
            <button type="button" class="pedido-card__corpo" @click="abrirDetalhe(p)">
              <div class="pedido-card__linha">
                <span class="pedido-card__id">#{{ p.id }}</span>
                <span class="pedido-card__hora">{{ horaMin(p.horarioPedido) }}</span>
              </div>
              <div class="pedido-card__tags">
                <span class="tag">{{ p.plataformaNome }}</span>
                <span class="tag tag--fraca">{{ p.formaDePagamentoNome }}</span>
              </div>
              <div class="pedido-card__itens">{{ resumoItens(p) }}</div>
              <div class="pedido-card__rodape">
                <span class="pedido-card__cliente">{{ p.nomeCliente || '—' }}</span>
                <span class="pedido-card__valor">{{ moeda(p.valor) }}</span>
              </div>
            </button>

            <div class="pedido-card__acoes">
              <button
                v-if="proximo(p.status, ehEntrega(p))"
                type="button"
                class="btn-avancar"
                @click="avancar(p)"
              >
                Avançar → {{ STATUS_LABEL[proximo(p.status, ehEntrega(p))!] }}
              </button>
              <select
                class="select-status"
                :value="p.status"
                aria-label="Mudar etapa"
                @change="aoEscolherStatus(p, $event)"
              >
                <option v-for="s in STATUS_TODOS" :key="s" :value="s">{{ STATUS_LABEL[s] }}</option>
              </select>
            </div>
          </article>
        </div>
      </section>
    </div>
  </template>

  <!-- Detalhes -->
  <BaseModal
    :aberto="!!detalhe"
    largo
    :titulo="detalhe ? `Pedido #${detalhe.id}` : ''"
    @fechar="detalhe = null"
  >
    <template v-if="detalhe">
      <p class="detalhe__meta">
        {{ horaMin(detalhe.horarioPedido) }} · {{ detalhe.plataformaNome }} ·
        {{ detalhe.formaDePagamentoNome }}
      </p>
      <p class="detalhe__meta">
        Status:
        <span class="badge" :class="detalhe.status === 'CANCELADO' ? '' : 'badge--on'">
          {{ STATUS_LABEL[detalhe.status] }}
        </span>
        · Cliente: {{ detalhe.nomeCliente || '—' }}
      </p>

      <div class="tabela-scroll">
        <table class="tabela">
          <thead>
            <tr>
              <th>Item</th>
              <th class="num">Qtd</th>
              <th class="num">Unit.</th>
              <th class="num">Subtotal</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(it, i) in detalhe.itens" :key="i">
              <td>{{ it.itemNome }}</td>
              <td class="num">{{ it.quantidade }}</td>
              <td class="num">{{ moeda(it.precoUnitario) }}</td>
              <td class="num">{{ moeda(it.subtotal) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="detalhe__total">
        Total: <strong>{{ moeda(detalhe.valor) }}</strong>
      </p>
    </template>
    <template #acoes>
      <BaseButton variante="fantasma" @click="detalhe = null">Fechar</BaseButton>
      <BaseButton
        v-if="detalhe && proximo(detalhe.status, ehEntrega(detalhe))"
        variante="fantasma"
        @click="avancar(detalhe)"
      >
        Avançar → {{ detalhe ? STATUS_LABEL[proximo(detalhe.status, ehEntrega(detalhe))!] : '' }}
      </BaseButton>
      <BaseButton variante="fantasma" @click="editarDoDetalhe">Editar</BaseButton>
      <BaseButton v-if="auth.isGerente" variante="perigo" @click="excluirDoDetalhe">
        Excluir
      </BaseButton>
    </template>
  </BaseModal>

  <!-- Edição -->
  <BaseModal
    :aberto="!!editando"
    largo
    :titulo="editando ? `Editar pedido #${editando.id}` : ''"
    @fechar="editando = null"
  >
    <div v-if="erroEdicao" class="alerta-erro">{{ erroEdicao }}</div>

    <div class="pilha">
      <BaseField rotulo="Plataforma">
        <select v-model.number="formPlataformaId">
          <option :value="null">Selecione…</option>
          <option v-for="p in plataformas" :key="p.id" :value="p.id">{{ p.nome }}</option>
        </select>
      </BaseField>
      <BaseField rotulo="Forma de pagamento">
        <select v-model.number="formFormaId">
          <option :value="null">Selecione…</option>
          <option v-for="f in formasPagamento" :key="f.id" :value="f.id">{{ f.nome }}</option>
        </select>
      </BaseField>
      <BaseField rotulo="Nome do cliente" dica="Opcional">
        <input v-model="formNomeCliente" type="text" />
      </BaseField>
    </div>

    <div class="editor-itens">
      <CarrinhoEditor v-model="carrinhoEdicao" :itens="itens" />
      <p class="detalhe__total">
        Total: <strong>{{ moeda(totalEdicao) }}</strong>
      </p>
    </div>

    <template #acoes>
      <BaseButton variante="fantasma" @click="editando = null">Cancelar</BaseButton>
      <BaseButton :carregando="salvando" @click="salvarEdicao">
        {{ salvando ? 'Salvando…' : 'Salvar' }}
      </BaseButton>
    </template>
  </BaseModal>
</template>

<style scoped>
.barra-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px;
  margin-bottom: 18px;
  font-size: 12px;
  color: var(--cor-texto-suave);
  background: var(--cor-superficie);
  border: 1px solid var(--cor-borda);
  border-radius: var(--raio);
  flex-wrap: wrap;
}

.barra-status__estado {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.barra-status__acoes {
  display: inline-flex;
  gap: 16px;
}

.ponto {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--cor-ok);
  box-shadow: 0 0 0 3px var(--cor-ok-superficie);
}

/* ── Quadro (kanban) ─────────────────────────────────────── */
.quadro {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  padding-bottom: 8px;
  align-items: start;
}

.coluna {
  flex: 0 0 288px;
  background: var(--cor-superficie);
  border: 1px solid var(--cor-borda);
  border-radius: var(--raio-lg);
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 300px);
  min-height: 160px;
}

.coluna__cabecalho {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--cor-borda);
}

.coluna__titulo {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 13px;
}

.coluna__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.coluna__contador {
  font-size: 12px;
  color: var(--cor-texto-fraco);
  background: var(--cor-superficie-3);
  border-radius: 999px;
  padding: 1px 8px;
  min-width: 22px;
  text-align: center;
}

.coluna__lista {
  padding: 10px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.coluna__vazio {
  color: var(--cor-borda-forte);
  text-align: center;
  padding: 14px 0;
}

/* ── Card ────────────────────────────────────────────────── */
.pedido-card {
  border: 1px solid var(--cor-borda);
  border-radius: var(--raio);
  background: var(--cor-superficie);
  overflow: hidden;
  transition: box-shadow var(--transicao);
}

.pedido-card:hover {
  box-shadow: var(--sombra-1);
}

.pedido-card__corpo {
  display: block;
  width: 100%;
  text-align: left;
  background: none;
  border: none;
  padding: 11px 12px;
  cursor: pointer;
}

.pedido-card__linha {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.pedido-card__id {
  font-weight: 700;
  font-size: 13px;
}

.pedido-card__hora {
  font-size: 12px;
  color: var(--cor-texto-fraco);
  font-variant-numeric: tabular-nums;
}

.pedido-card__tags {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  margin: 7px 0;
}

.tag {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 999px;
  border: 1px solid var(--cor-borda-forte);
  color: var(--cor-texto-suave);
  white-space: nowrap;
}

.tag--fraca {
  color: var(--cor-texto-fraco);
}

.pedido-card__itens {
  font-size: 12px;
  color: var(--cor-texto-suave);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pedido-card__rodape {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-top: 7px;
}

.pedido-card__cliente {
  font-size: 12px;
  color: var(--cor-texto-fraco);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pedido-card__valor {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.pedido-card__acoes {
  display: flex;
  gap: 6px;
  padding: 8px 10px;
  border-top: 1px solid var(--cor-borda);
}

.btn-avancar {
  flex: 1;
  background: var(--cor-primario);
  color: var(--cor-primario-contraste);
  border: none;
  border-radius: var(--raio-sm);
  padding: 6px 8px;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.btn-avancar:hover {
  background: var(--cor-primario-hover);
}

.select-status {
  width: auto;
  flex-shrink: 0;
  padding: 5px 22px 5px 8px;
  font-size: 11px;
  background-position: right 6px center;
}

/* ── Modal detalhe ───────────────────────────────────────── */
.detalhe__meta {
  font-size: 13px;
  color: var(--cor-texto-suave);
  margin-bottom: 4px;
}

.detalhe__meta:last-of-type {
  margin-bottom: 16px;
}

.detalhe__total {
  margin-top: 12px;
  text-align: right;
  font-size: 14px;
  color: var(--cor-texto-suave);
}

.detalhe__total strong {
  color: var(--cor-acento);
  font-size: 16px;
  margin-left: 6px;
}

.editor-itens {
  margin-top: 18px;
}
</style>
