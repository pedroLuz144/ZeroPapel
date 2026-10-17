<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseField from '@/components/BaseField.vue'
import BaseModal from '@/components/BaseModal.vue'
import CarrinhoEditor from '@/components/CarrinhoEditor.vue'
import { canaisDeVendaApi, formasPagamentoApi, itensApi, pedidosApi, plataformasApi } from '@/api'
import type {
  AcaoHistoricoPedido,
  CanalDeVendaResponse,
  HistoricoPedidoResponse,
  ItemResponse,
  PedidoResponse,
  PlataformaResponse,
  StatusPedido,
  TaxaResponse,
} from '@/api/types'
import type { LinhaCarrinho } from '@/types'
import { dataHora, dataLocalISO, horaMin, moeda } from '@/utils/formato'
import { auth } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

const toast = useToast()
const { confirmar } = useConfirm()

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

const ACAO_LABEL: Record<AcaoHistoricoPedido, string> = {
  CANCELADO: 'Cancelado',
  EDITADO: 'Editado',
  EXCLUIDO: 'Excluído',
}

const MINIMO_DO_MOTIVO = 3
const MAXIMO_DO_MOTIVO = 255

function ehStatusPedido(valor: string): valor is StatusPedido {
  return STATUS_TODOS.some((s) => s === valor)
}

function proximo(s: StatusPedido, ehEntrega: boolean): StatusPedido | null {
  const i = STATUS_ATIVOS.indexOf(s)
  if (i < 0 || i >= STATUS_ATIVOS.length - 1) return null
  const prox = STATUS_ATIVOS[i + 1]
  return prox === 'EM_ROTA' && !ehEntrega ? 'CONCLUIDO' : prox
}

function corDot(s: StatusPedido): string {
  if (s === 'EM_ABERTO') return 'var(--cor-acento)'
  if (s === 'PRONTO') return 'var(--cor-ok)'
  if (s === 'CANCELADO') return 'var(--cor-erro)'
  return 'var(--cor-texto-fraco)'
}

const pedidos = ref<PedidoResponse[]>([])
const plataformas = ref<PlataformaResponse[]>([])
const canais = ref<CanalDeVendaResponse[]>([])
const formasPagamento = ref<TaxaResponse[]>([])
const itens = ref<ItemResponse[]>([])

function ehEntrega(p: PedidoResponse): boolean {
  return plataformas.value.find((pl) => pl.id === p.plataformaId)?.entrega ?? true
}

const carregando = ref(true)
const erro = ref('')
const mostrarCancelados = ref(false)

const ultimaAtualizacao = ref(Date.now())
const agora = ref(Date.now())
let timer: number | undefined
let timerDeRecarga: number | undefined
let interacoes = 0
let acoesEmAndamento = 0
let ultimaCarga = 0

const INTERVALO_DE_RECARGA_MS = 60000

const detalhe = ref<PedidoResponse | null>(null)

const editando = ref<PedidoResponse | null>(null)
const formPlataformaId = ref<number | null>(null)

const plataformasDaEdicao = computed<PlataformaResponse[]>(() => {
  const pedido = editando.value
  if (!pedido) return []
  return plataformas.value.filter(
    (pl) =>
      pl.id === pedido.plataformaId ||
      canais.value.some(
        (c) => c.ativo && c.lojaId === pedido.lojaId && c.plataformaId === pl.id,
      ),
  )
})

const cancelando = ref<PedidoResponse | null>(null)
const motivo = ref('')
const erroCancelamento = ref('')
const enviandoCancelamento = ref(false)

const historicoDe = ref<PedidoResponse | null>(null)
const historico = ref<HistoricoPedidoResponse[]>([])
const carregandoHistorico = ref(false)
const erroHistorico = ref('')

const formFormaId = ref<number | null>(null)
const formNomeCliente = ref('')
const carrinhoEdicao = ref<LinhaCarrinho[]>([])
const erroEdicao = ref('')
const salvando = ref(false)

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

function irParaColuna(status: StatusPedido): void {
  document
    .getElementById(`coluna-${status}`)
    ?.scrollIntoView({ behavior: 'smooth', inline: 'start', block: 'nearest' })
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

async function carregar(): Promise<void> {
  const carga = ++ultimaCarga
  const interacoesNoInicio = interacoes
  try {
    const hoje = dataLocalISO()
    const lista = await pedidosApi.listar(`${hoje}T00:00:00`, `${hoje}T23:59:59`)
    if (carga !== ultimaCarga || interacoesNoInicio !== interacoes) return
    pedidos.value = lista
    erro.value = ''
    ultimaAtualizacao.value = Date.now()
    agora.value = Date.now()
  } catch (e) {
    if (carga !== ultimaCarga) return
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar os pedidos.'
  }
}

function telaOcupada(): boolean {
  return (
    detalhe.value !== null ||
    editando.value !== null ||
    cancelando.value !== null ||
    historicoDe.value !== null ||
    acoesEmAndamento > 0
  )
}

function recarregarSozinho(): void {
  if (!telaOcupada()) carregar()
}

async function comoAcaoDoUsuario(acao: () => Promise<void>): Promise<void> {
  interacoes += 1
  acoesEmAndamento += 1
  try {
    await acao()
  } finally {
    acoesEmAndamento -= 1
  }
}

onMounted(async () => {
  try {
    const [ped, pla, can, fpg, itn] = await Promise.all([
      pedidosApi.listar(),
      plataformasApi.listar(),
      canaisDeVendaApi.listar(),
      formasPagamentoApi.listar(),
      itensApi.listar(),
    ])
    pedidos.value = ped
    plataformas.value = pla
    canais.value = can
    formasPagamento.value = fpg
    itens.value = itn
    ultimaAtualizacao.value = Date.now()
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar os dados.'
  } finally {
    carregando.value = false
  }
  timer = window.setInterval(() => (agora.value = Date.now()), 10000)
  timerDeRecarga = window.setInterval(recarregarSozinho, INTERVALO_DE_RECARGA_MS)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (timerDeRecarga) clearInterval(timerDeRecarga)
})

function substituirPedido(atualizado: PedidoResponse): void {
  const i = pedidos.value.findIndex((x) => x.id === atualizado.id)
  if (i >= 0) pedidos.value[i] = atualizado
  if (detalhe.value?.id === atualizado.id) detalhe.value = atualizado
}

async function mudarStatus(p: PedidoResponse, novo: StatusPedido): Promise<void> {
  await comoAcaoDoUsuario(async () => {
    try {
      substituirPedido(await pedidosApi.atualizarStatus(p.id, { status: novo }))
      toast.sucesso(`Pedido #${p.id} → ${STATUS_LABEL[novo]}`)
    } catch (e) {
      toast.erro(e instanceof Error ? e.message : 'Erro ao mudar o status.')
    }
  })
}

function abrirCancelamento(p: PedidoResponse): void {
  if (!podeOperar(p)) return
  interacoes += 1
  cancelando.value = p
  motivo.value = ''
  erroCancelamento.value = ''
}

async function confirmarCancelamento(): Promise<void> {
  const p = cancelando.value
  if (!p) return
  const limpo = motivo.value.trim()
  if (limpo.length < MINIMO_DO_MOTIVO || limpo.length > MAXIMO_DO_MOTIVO) {
    erroCancelamento.value = `Informe o motivo, de ${MINIMO_DO_MOTIVO} a ${MAXIMO_DO_MOTIVO} caracteres.`
    return
  }
  enviandoCancelamento.value = true
  erroCancelamento.value = ''
  await comoAcaoDoUsuario(async () => {
    try {
      substituirPedido(await pedidosApi.atualizarStatus(p.id, { status: 'CANCELADO', motivo: limpo }))
      toast.sucesso(`Pedido #${p.id} cancelado.`)
      cancelando.value = null
    } catch (e) {
      erroCancelamento.value = e instanceof Error ? e.message : 'Erro ao cancelar o pedido.'
    } finally {
      enviandoCancelamento.value = false
    }
  })
}

async function abrirHistorico(p: PedidoResponse): Promise<void> {
  interacoes += 1
  detalhe.value = null
  historicoDe.value = p
  historico.value = []
  erroHistorico.value = ''
  carregandoHistorico.value = true
  try {
    const lista = await pedidosApi.historico(p.id)
    if (historicoDe.value?.id === p.id) historico.value = lista
  } catch (e) {
    erroHistorico.value = e instanceof Error ? e.message : 'Erro ao carregar o histórico.'
  } finally {
    carregandoHistorico.value = false
  }
}

function autorDoRegistro(r: HistoricoPedidoResponse): string {
  if (r.usuarioNome === null) return 'Plataforma'
  return r.usuarioLogin ? `${r.usuarioNome} (${r.usuarioLogin})` : r.usuarioNome
}

function ehExterno(p: PedidoResponse): boolean {
  return p.plataformaPedidoId !== null
}

function podeOperar(p: PedidoResponse): boolean {
  return !p.fechado && !ehExterno(p)
}

async function avancar(p: PedidoResponse): Promise<void> {
  if (!podeOperar(p)) return
  const prox = proximo(p.status, ehEntrega(p))
  if (prox) await mudarStatus(p, prox)
}

async function aoEscolherStatus(p: PedidoResponse, evento: Event): Promise<void> {
  const alvo = evento.target
  if (!(alvo instanceof HTMLSelectElement)) return
  const novo = alvo.value
  alvo.value = p.status
  if (!podeOperar(p) || !ehStatusPedido(novo) || novo === p.status) return
  if (novo === 'CANCELADO') {
    abrirCancelamento(p)
    return
  }
  await mudarStatus(p, novo)
}

function abrirDetalhe(p: PedidoResponse): void {
  interacoes += 1
  detalhe.value = p
}

function editarDoDetalhe(): void {
  const p = detalhe.value
  detalhe.value = null
  if (p) abrirEdicao(p)
}

function cancelarDoDetalhe(): void {
  const p = detalhe.value
  detalhe.value = null
  if (p) abrirCancelamento(p)
}

async function excluirDoDetalhe(): Promise<void> {
  const p = detalhe.value
  detalhe.value = null
  if (p) await excluir(p)
}

function podeEditar(p: PedidoResponse): boolean {
  return podeOperar(p) && p.itens.every((it) => it.itemId !== null)
}

function abrirEdicao(p: PedidoResponse): void {
  if (!podeEditar(p)) return
  const linhas: LinhaCarrinho[] = []
  for (const it of p.itens) {
    if (it.itemId === null) return
    linhas.push({
      itemId: it.itemId,
      nome: it.itemNome,
      preco: Number(it.precoUnitario),
      quantidade: it.quantidade,
    })
  }
  interacoes += 1
  editando.value = p
  formPlataformaId.value = p.plataformaId
  formFormaId.value = p.formaDePagamentoId
  formNomeCliente.value = p.nomeCliente ?? ''
  carrinhoEdicao.value = linhas
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

  const pedido = editando.value
  const corpo = {
    plataformaId: formPlataformaId.value,
    nomeCliente: formNomeCliente.value.trim() || null,
    formaDePagamentoId: formFormaId.value,
    itens: carrinhoEdicao.value.map((l) => ({ itemId: l.itemId, quantidade: l.quantidade })),
  }

  salvando.value = true
  erroEdicao.value = ''
  await comoAcaoDoUsuario(async () => {
    try {
      const atualizado = await pedidosApi.atualizar(pedido.id, corpo)
      const i = pedidos.value.findIndex((x) => x.id === atualizado.id)
      if (i >= 0) pedidos.value[i] = atualizado
      toast.sucesso(`Pedido #${pedido.id} atualizado.`)
      editando.value = null
    } catch (e) {
      erroEdicao.value = e instanceof Error ? e.message : 'Erro ao salvar o pedido.'
    } finally {
      salvando.value = false
    }
  })
}

async function excluir(p: PedidoResponse): Promise<void> {
  if (!podeOperar(p)) return
  await comoAcaoDoUsuario(async () => {
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
  })
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

    <nav class="atalhos" aria-label="Etapas">
      <button
        v-for="col in colunas"
        :key="col.status"
        type="button"
        class="atalho"
        @click="irParaColuna(col.status)"
      >
        <span class="coluna__dot" :style="{ background: corDot(col.status) }"></span>
        {{ col.label }}
        <span class="atalho__contador">{{ col.itens.length }}</span>
      </button>
    </nav>

    <div class="quadro">
      <section
        v-for="col in colunas"
        :id="`coluna-${col.status}`"
        :key="col.status"
        class="coluna"
      >
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
                <span v-if="p.fechado" class="badge">Fechado</span>
                <span
                  v-if="ehExterno(p)"
                  class="badge"
                  title="Pedido recebido de plataforma externa; o status é atualizado pela plataforma"
                >
                  Externo
                </span>
              </div>
              <div class="pedido-card__itens">{{ resumoItens(p) }}</div>
              <div v-if="p.status === 'CANCELADO' && p.motivoCancelamento" class="pedido-card__motivo">
                Motivo: {{ p.motivoCancelamento }}
              </div>
              <div class="pedido-card__rodape">
                <span class="pedido-card__cliente">{{ p.nomeCliente || '—' }}</span>
                <span class="pedido-card__valor">{{ moeda(p.valor) }}</span>
              </div>
            </button>

            <div v-if="podeOperar(p)" class="pedido-card__acoes">
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

  <BaseModal
    :aberto="!!detalhe"
    largo
    :titulo="detalhe ? `Pedido #${detalhe.id}` : ''"
    @fechar="detalhe = null"
  >
    <template v-if="detalhe">
      <p class="detalhe__meta">
        {{ horaMin(detalhe.horarioPedido) }} · {{ detalhe.lojaNome }} · {{ detalhe.plataformaNome }} ·
        {{ detalhe.formaDePagamentoNome }}
      </p>
      <p class="detalhe__meta">
        Status:
        <span class="badge" :class="detalhe.status === 'CANCELADO' ? '' : 'badge--on'">
          {{ STATUS_LABEL[detalhe.status] }}
        </span>
        <template v-if="detalhe.fechado">
          · <span class="badge">Fechado</span>
        </template>
        <template v-if="ehExterno(detalhe)">
          ·
          <span
            class="badge"
            title="Pedido recebido de plataforma externa; o status é atualizado pela plataforma"
          >
            Externo
          </span>
        </template>
        · Cliente: {{ detalhe.nomeCliente || '—' }}
      </p>
      <p v-if="detalhe.status === 'CANCELADO' && detalhe.motivoCancelamento" class="detalhe__meta">
        Motivo do cancelamento: {{ detalhe.motivoCancelamento }}
      </p>

      <div class="tabela-scroll">
        <table class="tabela">
          <thead>
            <tr>
              <th>Item</th>
              <th class="num">Qtd</th>
              <th class="num so-largo">Unit.</th>
              <th class="num">Subtotal</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(it, i) in detalhe.itens" :key="i">
              <td>{{ it.itemNome }}</td>
              <td class="num">{{ it.quantidade }}</td>
              <td class="num so-largo">{{ moeda(it.precoUnitario) }}</td>
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
        v-if="detalhe && podeOperar(detalhe) && proximo(detalhe.status, ehEntrega(detalhe))"
        variante="fantasma"
        @click="avancar(detalhe)"
      >
        Avançar → {{ detalhe ? STATUS_LABEL[proximo(detalhe.status, ehEntrega(detalhe))!] : '' }}
      </BaseButton>
      <BaseButton v-if="auth.isGerente && detalhe" variante="fantasma" @click="abrirHistorico(detalhe)">
        Histórico
      </BaseButton>
      <BaseButton v-if="detalhe && podeEditar(detalhe)" variante="fantasma" @click="editarDoDetalhe">
        Editar
      </BaseButton>
      <BaseButton
        v-if="detalhe && podeOperar(detalhe) && detalhe.status !== 'CANCELADO'"
        variante="perigo"
        @click="cancelarDoDetalhe"
      >
        Cancelar pedido
      </BaseButton>
      <BaseButton
        v-if="auth.isGerente && detalhe && podeOperar(detalhe)"
        variante="perigo"
        @click="excluirDoDetalhe"
      >
        Excluir
      </BaseButton>
    </template>
  </BaseModal>

  <BaseModal
    :aberto="!!cancelando"
    :titulo="cancelando ? `Cancelar pedido #${cancelando.id}` : ''"
    @fechar="cancelando = null"
  >
    <div v-if="erroCancelamento" class="alerta-erro">{{ erroCancelamento }}</div>
    <BaseField
      rotulo="Motivo do cancelamento"
      obrigatorio
      :dica="`De ${MINIMO_DO_MOTIVO} a ${MAXIMO_DO_MOTIVO} caracteres`"
    >
      <textarea v-model="motivo" rows="3" :maxlength="MAXIMO_DO_MOTIVO"></textarea>
    </BaseField>
    <template #acoes>
      <BaseButton variante="fantasma" @click="cancelando = null">Voltar</BaseButton>
      <BaseButton variante="perigo" :carregando="enviandoCancelamento" @click="confirmarCancelamento">
        {{ enviandoCancelamento ? 'Cancelando…' : 'Cancelar pedido' }}
      </BaseButton>
    </template>
  </BaseModal>

  <BaseModal
    :aberto="!!historicoDe"
    :titulo="historicoDe ? `Histórico do pedido #${historicoDe.id}` : ''"
    @fechar="historicoDe = null"
  >
    <div v-if="erroHistorico" class="alerta-erro">{{ erroHistorico }}</div>
    <p v-if="carregandoHistorico" class="historico__vazio">Carregando…</p>
    <p v-else-if="!erroHistorico && historico.length === 0" class="historico__vazio">
      Nenhum cancelamento, edição ou exclusão registrado.
    </p>
    <ol v-else class="historico">
      <li v-for="r in historico" :key="r.id" class="historico__item">
        <div class="historico__linha">
          <span class="badge">{{ ACAO_LABEL[r.acao] }}</span>
          <span class="historico__quando">{{ dataHora(r.dataHora) }}</span>
        </div>
        <div class="historico__autor">{{ autorDoRegistro(r) }}</div>
        <div class="historico__detalhe">{{ r.detalhe }}</div>
      </li>
    </ol>
    <template #acoes>
      <BaseButton variante="fantasma" @click="historicoDe = null">Fechar</BaseButton>
    </template>
  </BaseModal>

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
          <option v-for="p in plataformasDaEdicao" :key="p.id" :value="p.id">{{ p.nome }}</option>
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

.pedido-card__motivo {
  margin-top: 6px;
  font-size: 12px;
  color: var(--cor-erro);
  overflow-wrap: anywhere;
}

.historico {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.historico__item {
  border: 1px solid var(--cor-borda);
  border-radius: var(--raio);
  padding: 10px 12px;
}

.historico__linha {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.historico__quando {
  font-size: 12px;
  color: var(--cor-texto-fraco);
  font-variant-numeric: tabular-nums;
}

.historico__autor {
  margin-top: 6px;
  font-size: 13px;
  font-weight: 500;
}

.historico__detalhe {
  margin-top: 2px;
  font-size: 13px;
  color: var(--cor-texto-suave);
  overflow-wrap: anywhere;
}

.historico__vazio {
  color: var(--cor-texto-fraco);
  font-size: 13px;
}

.atalhos {
  display: none;
}

@media (max-width: 720px) {
  .atalhos {
    display: flex;
    gap: 6px;
    overflow-x: auto;
    scrollbar-width: none;
    margin: 0 -16px 12px;
    padding: 0 16px;
  }

  .atalhos::-webkit-scrollbar {
    display: none;
  }

  .atalho {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
    padding: 7px 12px;
    font-size: 12px;
    color: var(--cor-texto-suave);
    background: var(--cor-superficie);
    border: 1px solid var(--cor-borda);
    border-radius: 999px;
    cursor: pointer;
  }

  .atalho__contador {
    font-weight: 600;
    color: var(--cor-texto);
  }

  .quadro {
    scroll-snap-type: x mandatory;
    scroll-padding: 0 16px;
    margin: 0 -16px;
    padding: 0 16px 8px;
    gap: 10px;
  }

  .coluna {
    flex: 0 0 calc(100vw - 56px);
    max-height: none;
    scroll-snap-align: start;
  }

  .pedido-card__acoes {
    padding: 8px;
  }

  .btn-avancar {
    padding: 11px 10px;
    font-size: 13px;
  }

  .select-status {
    font-size: 16px;
    padding: 8px 26px 8px 10px;
  }
}
</style>
