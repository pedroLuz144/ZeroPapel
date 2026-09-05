<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseField from './BaseField.vue'
import BaseButton from './BaseButton.vue'
import type { ItemResponse } from '@/api/types'
import type { LinhaCarrinho } from '@/types'
import { moeda } from '@/utils/formato'

const props = defineProps<{
  modelValue: LinhaCarrinho[]
  itens: ItemResponse[]
}>()

const emit = defineEmits<{ 'update:modelValue': [LinhaCarrinho[]] }>()

const itemSelId = ref<number | null>(null)
const quantidade = ref(1)
const erro = ref('')

const itensAtivos = computed(() => props.itens.filter((i) => i.status === 'ATIVO'))

function adicionar(): void {
  erro.value = ''
  const item = props.itens.find((i) => i.id === itemSelId.value)
  if (!item) {
    erro.value = 'Selecione um item.'
    return
  }
  if (!Number.isFinite(quantidade.value) || quantidade.value < 1) {
    erro.value = 'Quantidade inválida.'
    return
  }

  const linhas = props.modelValue.map((l) => ({ ...l }))
  const existente = linhas.find((l) => l.itemId === item.id)
  if (existente) {
    existente.quantidade += quantidade.value
  } else {
    linhas.push({
      itemId: item.id,
      nome: item.nome,
      preco: Number(item.preco),
      quantidade: quantidade.value,
    })
  }
  emit('update:modelValue', linhas)

  itemSelId.value = null
  quantidade.value = 1
}

function remover(indice: number): void {
  const linhas = props.modelValue.slice()
  linhas.splice(indice, 1)
  emit('update:modelValue', linhas)
}
</script>

<template>
  <div>
    <div class="linha-add">
      <BaseField rotulo="Item" :erro="erro" class="linha-add__campo">
        <select v-model.number="itemSelId">
          <option :value="null">Selecione…</option>
          <option v-for="i in itensAtivos" :key="i.id" :value="i.id">
            {{ i.nome }} — {{ moeda(i.preco) }}
          </option>
        </select>
      </BaseField>
      <BaseField rotulo="Qtd" class="linha-add__qtd">
        <input v-model.number="quantidade" type="number" min="1" />
      </BaseField>
      <BaseButton variante="fantasma" @click="adicionar">Adicionar</BaseButton>
    </div>

    <div class="tabela-scroll">
      <table class="tabela">
        <thead>
          <tr>
            <th>Item</th>
            <th class="num">Qtd</th>
            <th class="num">Unit.</th>
            <th class="num">Subtotal</th>
            <th aria-label="ações"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="modelValue.length === 0">
            <td colspan="5" class="tabela__vazio">Nenhum item adicionado.</td>
          </tr>
          <tr v-for="(linha, idx) in modelValue" :key="linha.itemId">
            <td>{{ linha.nome }}</td>
            <td class="num">{{ linha.quantidade }}</td>
            <td class="num">{{ moeda(linha.preco) }}</td>
            <td class="num">{{ moeda(linha.preco * linha.quantidade) }}</td>
            <td class="num">
              <button class="link-acao link-acao--perigo" @click="remover(idx)">Remover</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.linha-add {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.linha-add__campo {
  flex: 1;
}

.linha-add__qtd {
  width: 72px;
  flex-shrink: 0;
}

.linha-add > .btn {
  margin-top: 22px;
}
</style>
