<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseField from '@/components/BaseField.vue'
import BaseModal from '@/components/BaseModal.vue'
import { categoriasApi, itensApi } from '@/api'
import type { CategoriaResponse, ItemResponse, StatusItem } from '@/api/types'
import { moeda } from '@/utils/formato'
import { auth } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

interface FormItem {
  nome: string
  categoria: number | null
  preco: string
  status: StatusItem
  descricao: string
}

const toast = useToast()
const { confirmar } = useConfirm()

const itens = ref<ItemResponse[]>([])
const categorias = ref<CategoriaResponse[]>([])
const carregando = ref(true)
const erro = ref('')

const modalAberto = ref(false)
const editando = ref<ItemResponse | null>(null)
const erroModal = ref('')
const salvando = ref(false)
const form = reactive<FormItem>({
  nome: '',
  categoria: null,
  preco: '',
  status: 'ATIVO',
  descricao: '',
})

async function carregar(): Promise<void> {
  erro.value = ''
  try {
    const [itn, cat] = await Promise.all([itensApi.listar(), categoriasApi.listar()])
    itens.value = itn
    categorias.value = cat
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar o cardápio.'
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)

function abrirNovo(): void {
  editando.value = null
  Object.assign(form, { nome: '', categoria: null, preco: '', status: 'ATIVO', descricao: '' })
  erroModal.value = ''
  modalAberto.value = true
}

function abrirEdicao(item: ItemResponse): void {
  editando.value = item
  Object.assign(form, {
    nome: item.nome,
    categoria: item.categoriaId,
    preco: String(item.preco),
    status: item.status,
    descricao: item.descricao ?? '',
  })
  erroModal.value = ''
  modalAberto.value = true
}

async function salvar(): Promise<void> {
  const preco = Number.parseFloat(form.preco)
  if (!form.nome.trim() || form.categoria == null || !Number.isFinite(preco)) {
    erroModal.value = 'Preencha nome, categoria e preço.'
    return
  }
  if (preco < 0.01) {
    erroModal.value = 'O preço deve ser maior que zero.'
    return
  }

  salvando.value = true
  erroModal.value = ''
  try {
    if (editando.value) {
      await itensApi.atualizar(editando.value.id, {
        nome: form.nome.trim(),
        categoria: form.categoria,
        preco,
        status: form.status,
        descricao: form.descricao.trim() || null,
      })
      toast.sucesso('Item atualizado.')
    } else {
      await itensApi.adicionar({
        nome: form.nome.trim(),
        categoria: form.categoria,
        preco,
        descricao: form.descricao.trim() || null,
      })
      toast.sucesso('Item adicionado.')
    }
    modalAberto.value = false
    await carregar()
  } catch (e) {
    erroModal.value = e instanceof Error ? e.message : 'Erro ao salvar o item.'
  } finally {
    salvando.value = false
  }
}

async function excluir(item: ItemResponse): Promise<void> {
  const ok = await confirmar(`Excluir "${item.nome}"? Esta ação não pode ser desfeita.`, {
    titulo: 'Excluir item',
    rotuloConfirmar: 'Excluir',
  })
  if (!ok) return
  try {
    await itensApi.excluir(item.id)
    toast.sucesso('Item excluído.')
    await carregar()
  } catch (e) {
    toast.erro(e instanceof Error ? e.message : 'Erro ao excluir o item.')
  }
}
</script>

<template>
  <div v-if="carregando" class="estado-tela">Carregando…</div>

  <template v-else>
    <div class="barra-acoes">
      <span class="contagem">
        {{ itens.length }} {{ itens.length === 1 ? 'item cadastrado' : 'itens cadastrados' }}
      </span>
      <BaseButton v-if="auth.isGerente" @click="abrirNovo">+ Novo item</BaseButton>
    </div>

    <div v-if="erro" class="alerta-erro">{{ erro }}</div>

    <div class="tabela-scroll">
      <table class="tabela">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Categoria</th>
            <th class="num">Preço</th>
            <th>Status</th>
            <th v-if="auth.isGerente" aria-label="ações"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="itens.length === 0 && !erro">
            <td :colspan="auth.isGerente ? 5 : 4" class="tabela__vazio">
              Nenhum item cadastrado.
            </td>
          </tr>
          <tr v-for="item in itens" :key="item.id">
            <td>{{ item.nome }}</td>
            <td>{{ item.categoriaNome }}</td>
            <td class="num">{{ moeda(item.preco) }}</td>
            <td>
              <span class="badge" :class="item.status === 'ATIVO' ? 'badge--on' : 'badge--off'">
                {{ item.status === 'ATIVO' ? 'Ativo' : 'Inativo' }}
              </span>
            </td>
            <td v-if="auth.isGerente" class="num">
              <button class="link-acao" @click="abrirEdicao(item)">Editar</button>
              <button class="link-acao link-acao--perigo" @click="excluir(item)">Excluir</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>

  <BaseModal
    :aberto="modalAberto"
    :titulo="editando ? 'Editar item' : 'Novo item'"
    @fechar="modalAberto = false"
  >
    <div v-if="erroModal" class="alerta-erro">{{ erroModal }}</div>

    <div class="pilha">
      <BaseField rotulo="Nome" obrigatorio>
        <input v-model="form.nome" type="text" placeholder="Ex: Frango Empanado" />
      </BaseField>
      <BaseField rotulo="Categoria" obrigatorio>
        <select v-model.number="form.categoria">
          <option :value="null">Selecione…</option>
          <option v-for="c in categorias" :key="c.id" :value="c.id">{{ c.nome }}</option>
        </select>
      </BaseField>
      <BaseField rotulo="Preço (R$)" obrigatorio>
        <input v-model="form.preco" type="number" step="0.01" min="0.01" placeholder="Ex: 18.00" />
      </BaseField>
      <BaseField v-if="editando" rotulo="Status">
        <select v-model="form.status">
          <option value="ATIVO">Ativo</option>
          <option value="INATIVO">Inativo</option>
        </select>
      </BaseField>
      <BaseField rotulo="Descrição" dica="Opcional">
        <textarea v-model="form.descricao" rows="3"></textarea>
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
