<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseField from '@/components/BaseField.vue'
import { dashboardApi, fechamentoApi } from '@/api'
import type { FechamentoCaixaListagem, FechamentoResponse } from '@/api/types'
import { comSegundos, dataHora, moeda, paraInputDateTime } from '@/utils/formato'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

const toast = useToast()
const { confirmar } = useConfirm()

const de = ref('')
const ate = ref('')

const atual = ref<FechamentoResponse | null>(null)
const historico = ref<FechamentoCaixaListagem[]>([])

const carregando = ref(true) // primeira carga
const atualizando = ref(false) // recarga silenciosa (poll / botão)
const fechando = ref(false) // POST /fechamento em andamento
const erro = ref('')

const janelaCustom = ref(false) // usuário aplicou um período manual
const fonteAoVivo = ref(true) // false quando exibindo um fechamento salvo
let timer: number | undefined

function hora(h: number): string {
  return `${String(h).padStart(2, '0')}:00`
}

function periodoDeHoje(): void {
  const agora = new Date()
  const inicio = new Date(agora)
  inicio.setHours(0, 0, 0, 0)
  de.value = paraInputDateTime(inicio)
  ate.value = paraInputDateTime(agora)
}

// ── Derivados ─────────────────────────────────────────────
const maxBrutoPlataforma = computed(() =>
  Math.max(1, ...(atual.value?.porPlataforma ?? []).map((l) => l.bruto)),
)

function alturaBarra(bruto: number): string {
  return `${Math.max(2, Math.round((bruto / maxBrutoPlataforma.value) * 100))}%`
}

const formasComPercentual = computed(() => {
  const linhas = atual.value?.porFormaDePagamento ?? []
  const total = linhas.reduce((s, l) => s + l.bruto, 0)
  return linhas.map((l) => ({
    ...l,
    pct: total > 0 ? Math.round((l.bruto / total) * 100) : 0,
  }))
})

const totalPlataformas = computed(() => {
  const linhas = atual.value?.porPlataforma ?? []
  return {
    qtd: linhas.reduce((s, l) => s + l.qtdPedidos, 0),
    bruto: linhas.reduce((s, l) => s + l.bruto, 0),
    taxaPlataforma: linhas.reduce((s, l) => s + l.taxaPlataforma, 0),
    taxaPagamento: linhas.reduce((s, l) => s + l.taxaPagamento, 0),
    liquido: linhas.reduce((s, l) => s + l.liquido, 0),
  }
})

// ── Carga ─────────────────────────────────────────────────
async function carregarDashboard(custom: boolean, silencioso = false): Promise<void> {
  erro.value = ''
  if (custom) {
    if (!de.value || !ate.value) {
      erro.value = 'Informe o início e o fim do período.'
      return
    }
    if (de.value >= ate.value) {
      erro.value = 'O início do período deve ser anterior ao fim.'
      return
    }
  }

  janelaCustom.value = custom
  fonteAoVivo.value = true
  if (silencioso) atualizando.value = true
  try {
    atual.value = custom
      ? await dashboardApi.periodo(comSegundos(de.value), comSegundos(ate.value))
      : await dashboardApi.hoje()
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar o dashboard.'
  } finally {
    carregando.value = false
    atualizando.value = false
  }
}

function verHoje(): void {
  periodoDeHoje()
  carregarDashboard(false)
}

function atualizarAgora(): void {
  carregarDashboard(janelaCustom.value, true)
}

async function carregarHistorico(): Promise<void> {
  try {
    historico.value = await fechamentoApi.listar()
  } catch {
    /* histórico é secundário — silencioso */
  }
}

onMounted(() => {
  periodoDeHoje()
  carregarDashboard(false)
  carregarHistorico()
  timer = window.setInterval(() => {
    if (fonteAoVivo.value) carregarDashboard(janelaCustom.value, true)
  }, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// ── Fechar caixa (persiste) ──────────────────────────────
async function fecharCaixa(): Promise<void> {
  if (!de.value || !ate.value || de.value >= ate.value) {
    toast.erro('Ajuste o período no filtro acima antes de fechar o caixa.')
    return
  }
  const ok = await confirmar(
    `Fechar o caixa de ${dataHora(comSegundos(de.value))} a ${dataHora(comSegundos(ate.value))}? ` +
      'Isso grava um registro permanente e não pode ser desfeito pela tela.',
    { titulo: 'Fechar caixa', rotuloConfirmar: 'Fechar caixa' },
  )
  if (!ok) return

  fechando.value = true
  try {
    const salvo = await fechamentoApi.realizar({
      de: comSegundos(de.value),
      ate: comSegundos(ate.value),
    })
    toast.sucesso('Caixa fechado — registro salvo.')
    atual.value = salvo
    fonteAoVivo.value = false
    await carregarHistorico()
  } catch (e) {
    toast.erro(e instanceof Error ? e.message : 'Erro ao fechar o caixa.')
  } finally {
    fechando.value = false
  }
}

async function abrir(id: number): Promise<void> {
  erro.value = ''
  try {
    atual.value = await fechamentoApi.buscar(id)
    fonteAoVivo.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (e) {
    toast.erro(e instanceof Error ? e.message : 'Erro ao abrir o fechamento.')
  }
}
</script>

<template>
  <div class="cabecalho-dash">
    <p v-if="atual" class="janela">
      <span class="janela__tag" :class="fonteAoVivo ? 'janela__tag--vivo' : 'janela__tag--salvo'">
        {{ fonteAoVivo ? 'Ao vivo' : 'Fechamento salvo' }}
      </span>
      {{ dataHora(atual.de) }} → {{ dataHora(atual.ate) }}
    </p>
    <button class="link-acao" :disabled="atualizando" @click="atualizarAgora">
      {{ atualizando ? 'Atualizando…' : 'Atualizar' }}
    </button>
  </div>

  <div class="periodo">
    <BaseField rotulo="De">
      <input v-model="de" type="datetime-local" />
    </BaseField>
    <BaseField rotulo="Até">
      <input v-model="ate" type="datetime-local" />
    </BaseField>
    <BaseButton variante="fantasma" @click="verHoje">Hoje</BaseButton>
    <BaseButton variante="fantasma" @click="carregarDashboard(true)">Ver período</BaseButton>
  </div>

  <div v-if="erro" class="alerta-erro">{{ erro }}</div>
  <div v-if="carregando" class="estado-tela">Carregando…</div>

  <template v-else-if="atual">
    <div class="metricas">
      <div class="metrica">
        <div class="metrica__rotulo">Faturamento bruto</div>
        <div class="metrica__valor">{{ moeda(atual.resumo.faturamentoBruto) }}</div>
      </div>
      <div class="metrica">
        <div class="metrica__rotulo">Total de taxas</div>
        <div class="metrica__valor">− {{ moeda(atual.resumo.totalTaxas) }}</div>
      </div>
      <div class="metrica">
        <div class="metrica__rotulo">Faturamento líquido</div>
        <div class="metrica__valor">{{ moeda(atual.resumo.faturamentoLiquido) }}</div>
      </div>
      <div class="metrica">
        <div class="metrica__rotulo">Ticket médio</div>
        <div class="metrica__valor">{{ moeda(atual.resumo.ticketMedio) }}</div>
      </div>
    </div>

    <p v-if="atual.resumo.totalPedidos === 0" class="estado-tela">
      Nenhum pedido nesse período ainda.
    </p>

    <template v-else>
      <div class="paineis paineis--grafico">
        <section class="painel">
          <h2 class="painel__titulo">Faturamento por plataforma</h2>
          <div class="grafico">
            <div v-for="l in atual.porPlataforma" :key="l.plataforma" class="grafico__col">
              <span class="grafico__valor">{{ moeda(l.bruto) }}</span>
              <div class="grafico__trilha">
                <div class="grafico__barra" :style="{ height: alturaBarra(l.bruto) }"></div>
              </div>
              <span class="grafico__rotulo">{{ l.plataforma }}</span>
            </div>
          </div>
        </section>

        <section class="painel">
          <h2 class="painel__titulo">Formas de pagamento</h2>
          <ul class="legenda">
            <li v-for="l in formasComPercentual" :key="l.formaDePagamento" class="legenda__item">
              <span class="legenda__ponto" aria-hidden="true"></span>
              <span class="legenda__nome">{{ l.formaDePagamento }}</span>
              <span class="legenda__pct">{{ l.pct }}%</span>
              <span class="legenda__valor num">{{ moeda(l.bruto) }}</span>
            </li>
          </ul>
        </section>
      </div>

      <section class="painel bloco">
        <h2 class="painel__titulo">Detalhamento por plataforma</h2>
        <div class="tabela-scroll">
          <table class="tabela">
            <thead>
              <tr>
                <th>Plataforma</th>
                <th class="num">Pedidos</th>
                <th class="num">Bruto</th>
                <th class="num">Taxa plataforma</th>
                <th class="num">Taxa pagamento</th>
                <th class="num">Líquido</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="l in atual.porPlataforma" :key="l.plataforma">
                <td>{{ l.plataforma }}</td>
                <td class="num">{{ l.qtdPedidos }}</td>
                <td class="num">{{ moeda(l.bruto) }}</td>
                <td class="num">{{ moeda(l.taxaPlataforma) }}</td>
                <td class="num">{{ moeda(l.taxaPagamento) }}</td>
                <td class="num">{{ moeda(l.liquido) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td>Total</td>
                <td class="num">{{ totalPlataformas.qtd }}</td>
                <td class="num">{{ moeda(totalPlataformas.bruto) }}</td>
                <td class="num">{{ moeda(totalPlataformas.taxaPlataforma) }}</td>
                <td class="num">{{ moeda(totalPlataformas.taxaPagamento) }}</td>
                <td class="num">{{ moeda(totalPlataformas.liquido) }}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>

      <div class="paineis bloco">
        <section class="painel">
          <h2 class="painel__titulo">Itens mais vendidos</h2>
          <div class="tabela-scroll">
            <table class="tabela">
              <thead>
                <tr>
                  <th>Item</th>
                  <th class="num">Qtd</th>
                  <th class="num">Receita</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="atual.itensMaisVendidos.length === 0">
                  <td colspan="3" class="tabela__vazio">Sem dados no período.</td>
                </tr>
                <tr v-for="it in atual.itensMaisVendidos" :key="it.itemId">
                  <td>{{ it.itemNome }}</td>
                  <td class="num">{{ it.quantidadeTotal }}</td>
                  <td class="num">{{ moeda(it.receitaTotal) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="painel">
          <h2 class="painel__titulo">Pedidos por hora</h2>
          <div class="tabela-scroll">
            <table class="tabela">
              <thead>
                <tr>
                  <th>Hora</th>
                  <th class="num">Pedidos</th>
                  <th class="num">Bruto</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="atual.pedidosPorHora.length === 0">
                  <td colspan="3" class="tabela__vazio">Sem dados no período.</td>
                </tr>
                <tr v-for="h in atual.pedidosPorHora" :key="h.hora">
                  <td>{{ hora(h.hora) }}</td>
                  <td class="num">{{ h.qtdPedidos }}</td>
                  <td class="num">{{ moeda(h.faturamentoBruto) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </template>
  </template>

  <section class="painel bloco">
    <div class="painel__cabecalho">
      <h2 class="painel__titulo">Fechamento de caixa</h2>
      <BaseButton :carregando="fechando" @click="fecharCaixa">Fechar caixa do período</BaseButton>
    </div>
    <p class="ajuda">
      Grava um registro permanente do período selecionado no filtro acima. O dashboard é só leitura
      e recalcula sempre a partir dos pedidos atuais.
    </p>
    <div class="tabela-scroll">
      <table class="tabela">
        <thead>
          <tr>
            <th>Período</th>
            <th>Gerado em</th>
            <th>Gerado por</th>
            <th class="num">Pedidos</th>
            <th class="num">Bruto</th>
            <th class="num">Taxas</th>
            <th class="num">Líquido</th>
            <th class="num">Ticket médio</th>
            <th aria-label="ações"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="historico.length === 0">
            <td colspan="9" class="tabela__vazio">Nenhum fechamento gerado ainda.</td>
          </tr>
          <tr v-for="fc in historico" :key="fc.id">
            <td>{{ dataHora(fc.de) }} → {{ dataHora(fc.ate) }}</td>
            <td>{{ dataHora(fc.geradoEm) }}</td>
            <td>{{ fc.geradoPorNome }}</td>
            <td class="num">{{ fc.totalPedidos }}</td>
            <td class="num">{{ moeda(fc.faturamentoBruto) }}</td>
            <td class="num">{{ moeda(fc.totalTaxas) }}</td>
            <td class="num">{{ moeda(fc.faturamentoLiquido) }}</td>
            <td class="num">{{ moeda(fc.ticketMedio) }}</td>
            <td class="num"><button class="link-acao" @click="abrir(fc.id)">Abrir</button></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.cabecalho-dash {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.janela {
  font-size: 12px;
  color: var(--cor-texto-suave);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.janela__tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--cor-borda-forte);
}

.janela__tag--vivo {
  color: var(--cor-ok);
  border-color: var(--cor-ok-borda);
  background: var(--cor-ok-superficie);
}

.janela__tag--salvo {
  color: var(--cor-acento);
  border-color: var(--cor-acento-fraco);
  background: var(--cor-acento-superficie);
}

.periodo {
  display: flex;
  gap: 14px;
  align-items: flex-end;
  flex-wrap: wrap;
  margin-bottom: 22px;
}

.bloco {
  margin-top: 18px;
}

.ajuda {
  font-size: 12px;
  color: var(--cor-texto-fraco);
  margin: -6px 0 16px;
}

.paineis {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.paineis--grafico {
  grid-template-columns: minmax(0, 1.7fr) minmax(0, 1fr);
}

@media (max-width: 900px) {
  .paineis,
  .paineis--grafico {
    grid-template-columns: 1fr;
  }
}

/* ── Gráfico de barras ───────────────────────────────────── */
.grafico {
  display: flex;
  align-items: flex-end;
  gap: 28px;
  height: 200px;
  padding-top: 8px;
  overflow-x: auto;
}

.grafico__col {
  flex: 0 1 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.grafico__valor {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
  font-variant-numeric: tabular-nums;
}

.grafico__trilha {
  flex: 1;
  width: 46px;
  display: flex;
  align-items: flex-end;
  background: var(--cor-superficie-3);
  border-radius: var(--raio-sm) var(--raio-sm) 0 0;
}

.grafico__barra {
  width: 100%;
  background: var(--cor-acento);
  border-radius: var(--raio-sm) var(--raio-sm) 0 0;
  transition: height var(--transicao);
}

.grafico__rotulo {
  margin-top: 8px;
  font-size: 12px;
  color: var(--cor-texto-suave);
  text-align: center;
}

/* ── Legenda de formas de pagamento ──────────────────────── */
.legenda {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.legenda__item {
  display: grid;
  grid-template-columns: 14px 1fr auto auto;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--cor-borda);
  font-size: 13px;
}

.legenda__item:last-child {
  border-bottom: none;
}

.legenda__ponto {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--cor-acento);
}

.legenda__pct {
  color: var(--cor-texto-fraco);
  font-variant-numeric: tabular-nums;
}

.legenda__valor {
  font-weight: 600;
  min-width: 92px;
}
</style>
