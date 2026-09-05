<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BaseField from '@/components/BaseField.vue'
import BaseButton from '@/components/BaseButton.vue'
import CarrinhoEditor from '@/components/CarrinhoEditor.vue'
import { formasPagamentoApi, itensApi, pedidosApi, plataformasApi } from '@/api'
import type { ItemResponse, PlataformaResponse, TaxaResponse } from '@/api/types'
import type { LinhaCarrinho } from '@/types'
import { moeda } from '@/utils/formato'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const plataformas = ref<PlataformaResponse[]>([])
const formasPagamento = ref<TaxaResponse[]>([])
const itens = ref<ItemResponse[]>([])

const carregando = ref(true)
const erroCarregamento = ref('')

const formaDePagamentoId = ref<number | null>(null)
const nomeCliente = ref('')
const carrinho = ref<LinhaCarrinho[]>([])

const salvando = ref(false)

// O PDV só registra pedidos de balcão: a plataforma sem flag de entrega
// (fallback: uma chamada "Balcão"). Delivery entra pela integração.
const plataformaBalcao = computed<PlataformaResponse | null>(() => {
  const semEntrega = plataformas.value.find((p) => !p.entrega)
  if (semEntrega) return semEntrega
  return plataformas.value.find((p) => /balc[aã]o/i.test(p.nome)) ?? null
})

const total = computed(() =>
  carrinho.value.reduce((soma, l) => soma + l.preco * l.quantidade, 0),
)

onMounted(async () => {
  try {
    const [p, f, i] = await Promise.all([
      plataformasApi.listar(),
      formasPagamentoApi.listar(),
      itensApi.listar(),
    ])
    plataformas.value = p
    formasPagamento.value = f
    itens.value = i
  } catch {
    erroCarregamento.value =
      'Não foi possível carregar plataformas, formas de pagamento ou o cardápio.'
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

    <section class="painel">
      <h2 class="painel__titulo">Dados do pedido</h2>
      <div class="pilha">
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
      <BaseButton :carregando="salvando" :desabilitado="!plataformaBalcao" @click="registrar">
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
