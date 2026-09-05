<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseField from '@/components/BaseField.vue'
import BaseModal from '@/components/BaseModal.vue'
import { formasPagamentoApi } from '@/api'
import type { TaxaResponse } from '@/api/types'
import { percentual } from '@/utils/formato'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

const toast = useToast()
const { confirmar } = useConfirm()

const registros = ref<TaxaResponse[]>([])
const carregando = ref(true)
const erro = ref('')

const modalAberto = ref(false)
const editando = ref<TaxaResponse | null>(null)
const erroModal = ref('')
const salvando = ref(false)
const form = reactive({ nome: '', taxaPercentual: '' })

const contagem = computed(() => {
  const n = registros.value.length
  return `${n} ${n === 1 ? 'forma cadastrada' : 'formas cadastradas'}`
})

async function carregar(): Promise<void> {
  erro.value = ''
  try {
    registros.value = await formasPagamentoApi.listar()
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar as formas de pagamento.'
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)

function abrir(registro?: TaxaResponse): void {
  editando.value = registro ?? null
  Object.assign(form, {
    nome: registro?.nome ?? '',
    taxaPercentual: registro ? String(registro.taxaPercentual) : '',
  })
  erroModal.value = ''
  modalAberto.value = true
}

async function salvar(): Promise<void> {
  const taxa = Number.parseFloat(form.taxaPercentual)
  if (!form.nome.trim() || !Number.isFinite(taxa)) {
    erroModal.value = 'Preencha o nome e a taxa percentual.'
    return
  }
  if (taxa < 0) {
    erroModal.value = 'A taxa não pode ser negativa.'
    return
  }

  salvando.value = true
  erroModal.value = ''
  try {
    const body = { nome: form.nome.trim(), taxaPercentual: taxa }
    if (editando.value) {
      await formasPagamentoApi.atualizar(editando.value.id, body)
      toast.sucesso('Forma de pagamento atualizada.')
    } else {
      await formasPagamentoApi.adicionar(body)
      toast.sucesso('Forma de pagamento adicionada.')
    }
    modalAberto.value = false
    await carregar()
  } catch (e) {
    erroModal.value = e instanceof Error ? e.message : 'Erro ao salvar.'
  } finally {
    salvando.value = false
  }
}

async function excluir(registro: TaxaResponse): Promise<void> {
  const ok = await confirmar(`Excluir a forma de pagamento "${registro.nome}"?`, {
    titulo: 'Excluir',
    rotuloConfirmar: 'Excluir',
  })
  if (!ok) return
  try {
    await formasPagamentoApi.excluir(registro.id)
    toast.sucesso('Forma de pagamento excluída.')
    await carregar()
  } catch (e) {
    toast.erro(e instanceof Error ? e.message : 'Erro ao excluir.')
  }
}
</script>

<template>
  <div v-if="carregando" class="estado-tela">Carregando…</div>

  <template v-else>
    <div class="barra-acoes">
      <span class="contagem">{{ contagem }}</span>
      <BaseButton @click="abrir()">+ Nova forma</BaseButton>
    </div>

    <div v-if="erro" class="alerta-erro">{{ erro }}</div>

    <div class="tabela-scroll">
      <table class="tabela">
        <thead>
          <tr>
            <th>Nome</th>
            <th class="num">Taxa</th>
            <th aria-label="ações"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="registros.length === 0 && !erro">
            <td colspan="3" class="tabela__vazio">Nenhuma forma de pagamento cadastrada.</td>
          </tr>
          <tr v-for="r in registros" :key="r.id">
            <td>{{ r.nome }}</td>
            <td class="num">{{ percentual(r.taxaPercentual) }}</td>
            <td class="num">
              <button class="link-acao" @click="abrir(r)">Editar</button>
              <button class="link-acao link-acao--perigo" @click="excluir(r)">Excluir</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>

  <BaseModal
    :aberto="modalAberto"
    :titulo="editando ? 'Editar forma de pagamento' : 'Nova forma de pagamento'"
    @fechar="modalAberto = false"
  >
    <div v-if="erroModal" class="alerta-erro">{{ erroModal }}</div>

    <div class="pilha">
      <BaseField rotulo="Nome" obrigatorio>
        <input v-model="form.nome" type="text" placeholder="Ex: Cartão de Crédito" />
      </BaseField>
      <BaseField rotulo="Taxa percentual (%)" obrigatorio>
        <input
          v-model="form.taxaPercentual"
          type="number"
          step="0.01"
          min="0"
          placeholder="Ex: 3.50"
        />
      </BaseField>
    </div>

    <template #acoes>
      <BaseButton variante="fantasma" @click="modalAberto = false">Cancelar</BaseButton>
      <BaseButton :carregando="salvando" @click="salvar">
        {{ salvando ? 'Salvando…' : 'Salvar' }}
      </BaseButton>
    </template>
  </BaseModal>
</template>
