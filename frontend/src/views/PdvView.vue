<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import BaseField from '@/components/BaseField.vue'
import BaseButton from '@/components/BaseButton.vue'
import CarrinhoEditor from '@/components/CarrinhoEditor.vue'
import {
  canaisDeVendaApi,
  formasPagamentoApi,
  itensApi,
  lojasApi,
  pedidosApi,
  plataformasApi,
} from '@/api'
import type {
  CanalDeVendaResponse,
  ItemResponse,
  LojaResponse,
  PlataformaResponse,
  TaxaResponse,
} from '@/api/types'
import type { LinhaCarrinho } from '@/types'
import { moeda } from '@/utils/formato'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const CHAVE_LOJA = 'zeropapel.pdv.loja'

const lojas = ref<LojaResponse[]>([])
const lojaId = ref<number | null>(null)
const plataformas = ref<PlataformaResponse[]>([])
const canais = ref<CanalDeVendaResponse[]>([])
const formasPagamento = ref<TaxaResponse[]>([])
const itens = ref<ItemResponse[]>([])

const carregando = ref(true)
const erroCarregamento = ref('')

const formaDePagamentoId = ref<number | null>(null)
const nomeCliente = ref('')
const carrinho = ref<LinhaCarrinho[]>([])

const salvando = ref(false)

const plataformaBalcao = computed<PlataformaResponse | null>(() => {
  const semEntrega = plataformas.value.find((p) => !p.entrega)
  if (semEntrega) return semEntrega
  return plataformas.value.find((p) => /balc[aã]o/i.test(p.nome)) ?? null
})

const lojasAtivas = computed(() => lojas.value.filter((l) => l.ativo))

const semLojaAtiva = computed(() => lojasAtivas.value.length === 0)

const lojaSemCanalDeBalcao = computed(() => {
  const balcao = plataformaBalcao.value
  const loja = lojaId.value
  if (!balcao || loja == null) return false
  return !canais.value.some((c) => c.ativo && c.lojaId === loja && c.plataformaId === balcao.id)
})

function lojaLembrada(): number | null {
  try {
    const id = Number(localStorage.getItem(CHAVE_LOJA))
    return lojasAtivas.value.some((l) => l.id === id) ? id : null
  } catch {
    return null
  }
}

function escolherLojaInicial(): void {
  const unica = lojasAtivas.value.length === 1 ? lojasAtivas.value[0] : undefined
  lojaId.value = unica ? unica.id : lojaLembrada()
}

watch(lojaId, (id) => {
  if (id == null) return
  try {
    localStorage.setItem(CHAVE_LOJA, String(id))
  } catch {
    return
  }
})

const total = computed(() =>
  carrinho.value.reduce((soma, l) => soma + l.preco * l.quantidade, 0),
)

onMounted(async () => {
  try {
    const [l, p, c, f, i] = await Promise.all([
      lojasApi.listar(),
      plataformasApi.listar(),
      canaisDeVendaApi.listar(),
      formasPagamentoApi.listar(),
      itensApi.listar(),
    ])
    lojas.value = l
    plataformas.value = p
    canais.value = c
    formasPagamento.value = f
    itens.value = i
    escolherLojaInicial()
  } catch {
    erroCarregamento.value =
      'Não foi possível carregar lojas, plataformas, canais de venda, formas de pagamento ou o cardápio.'
  } finally {
    carregando.value = false
  }
})

async function registrar(): Promise<void> {
  const plataforma = plataformaBalcao.value
  if (!plataforma) {
    toast.erro('Nenhuma plataforma de balcão configurada.')
    return
  }
  if (lojaId.value == null) {
    toast.erro('Selecione a loja.')
    return
  }
  if (formaDePagamentoId.value == null) {
    toast.erro('Selecione a forma de pagamento.')
    return
  }
  if (carrinho.value.length === 0) {
    toast.erro('Adicione pelo menos um item ao pedido.')
    return
  }

  salvando.value = true
  try {
    const pedido = await pedidosApi.registrar({
      lojaId: lojaId.value,
      plataformaId: plataforma.id,
      nomeCliente: nomeCliente.value.trim() || null,
      formaDePagamentoId: formaDePagamentoId.value,
      itens: carrinho.value.map((l) => ({ itemId: l.itemId, quantidade: l.quantidade })),
    })
    toast.sucesso(`Pedido #${pedido.id} registrado — ${moeda(pedido.valor)}.`)
    formaDePagamentoId.value = null
    nomeCliente.value = ''
    carrinho.value = []
  } catch (e) {
    toast.erro(e instanceof Error ? e.message : 'Erro ao registrar o pedido.')
  } finally {
    salvando.value = false
  }
}
</script>

<template>
  <div v-if="carregando" class="estado-tela">Carregando…</div>
  <div v-else-if="erroCarregamento" class="alerta-erro">{{ erroCarregamento }}</div>

  <div v-else class="pdv">
    <div
      v-if="!plataformaBalcao"
      class="alerta-erro"
      style="grid-column: 1 / -1"
    >
      Nenhuma plataforma de balcão configurada. Em Configurações → Plataformas, cadastre uma
      (ou edite a existente) deixando "Canal de entrega" desmarcado.
    </div>

    <div
      v-else-if="semLojaAtiva"
      class="alerta-erro"
      style="grid-column: 1 / -1"
    >
      Nenhuma loja ativa. Em Configurações → Lojas, ative ou cadastre uma.
    </div>

    <div
      v-else-if="lojaSemCanalDeBalcao"
      class="alerta-erro"
      style="grid-column: 1 / -1"
    >
      Esta loja não tem canal de balcão ativo. Em Configurações → Canais de venda, cadastre ou
      reative o canal de {{ plataformaBalcao.nome }} desta loja.
    </div>

    <section class="painel">
      <h2 class="painel__titulo">Dados do pedido</h2>
      <div class="pilha">
        <BaseField v-if="lojasAtivas.length > 1" rotulo="Loja" obrigatorio>
          <select v-model.number="lojaId">
            <option :value="null">Selecione…</option>
            <option v-for="l in lojasAtivas" :key="l.id" :value="l.id">{{ l.nome }}</option>
          </select>
        </BaseField>

        <BaseField rotulo="Plataforma">
          <input :value="plataformaBalcao?.nome ?? 'Balcão'" type="text" disabled />
        </BaseField>

        <BaseField rotulo="Forma de pagamento" obrigatorio>
          <select v-model.number="formaDePagamentoId">
            <option :value="null">Selecione…</option>
            <option v-for="f in formasPagamento" :key="f.id" :value="f.id">{{ f.nome }}</option>
          </select>
        </BaseField>

        <BaseField rotulo="Nome do cliente" dica="Opcional">
          <input v-model="nomeCliente" type="text" placeholder="Ex: Maria" />
        </BaseField>
      </div>
    </section>

    <section class="painel">
      <h2 class="painel__titulo">Itens</h2>
      <CarrinhoEditor v-model="carrinho" :itens="itens" />
      <p class="total">
        Total: <strong>{{ moeda(total) }}</strong>
      </p>
    </section>

    <div class="pdv__rodape">
      <BaseButton
        :carregando="salvando"
        :desabilitado="!plataformaBalcao || semLojaAtiva || lojaSemCanalDeBalcao"
        @click="registrar"
      >
        {{ salvando ? 'Registrando…' : 'Registrar pedido' }}
      </BaseButton>
    </div>
  </div>
</template>

<style scoped>
.pdv {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px;
  align-items: start;
}

.pdv__rodape {
  grid-column: 1 / -1;
}

@media (max-width: 900px) {
  .pdv {
    grid-template-columns: 1fr;
  }
}

.total {
  margin-top: 14px;
  text-align: right;
  font-size: 14px;
  color: var(--cor-texto-suave);
}

.total strong {
  color: var(--cor-acento);
  font-size: 16px;
  margin-left: 6px;
}
</style>
