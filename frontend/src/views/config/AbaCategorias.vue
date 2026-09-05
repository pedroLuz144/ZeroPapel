<script setup lang="ts">
import { onMounted, ref } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseField from '@/components/BaseField.vue'
import BaseModal from '@/components/BaseModal.vue'
import { categoriasApi } from '@/api'
import type { CategoriaResponse } from '@/api/types'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

const toast = useToast()
const { confirmar } = useConfirm()

const categorias = ref<CategoriaResponse[]>([])
const carregando = ref(true)
const erro = ref('')

const modalAberto = ref(false)
const editando = ref<CategoriaResponse | null>(null)
const nome = ref('')
const erroModal = ref('')
const salvando = ref(false)

async function carregar(): Promise<void> {
  erro.value = ''
  try {
    categorias.value = await categoriasApi.listar()
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar as categorias.'
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)

function abrir(categoria?: CategoriaResponse): void {
  editando.value = categoria ?? null
  nome.value = categoria?.nome ?? ''
  erroModal.value = ''
  modalAberto.value = true
}

async function salvar(): Promise<void> {
  if (!nome.value.trim()) {
    erroModal.value = 'Informe o nome da categoria.'
    return
  }
  salvando.value = true
  erroModal.value = ''
  try {
    const body = { nome: nome.value.trim() }
    if (editando.value) {
      await categoriasApi.atualizar(editando.value.id, body)
      toast.sucesso('Categoria atualizada.')
    } else {
      await categoriasApi.adicionar(body)
      toast.sucesso('Categoria adicionada.')
    }
    modalAberto.value = false
    await carregar()
  } catch (e) {
    erroModal.value = e instanceof Error ? e.message : 'Erro ao salvar a categoria.'
  } finally {
    salvando.value = false
  }
}

async function excluir(categoria: CategoriaResponse): Promise<void> {
  const ok = await confirmar(`Excluir a categoria "${categoria.nome}"?`, {
    titulo: 'Excluir categoria',
    rotuloConfirmar: 'Excluir',
  })
  if (!ok) return
  try {
    await categoriasApi.excluir(categoria.id)
    toast.sucesso('Categoria excluída.')
    await carregar()
  } catch (e) {
    toast.erro(e instanceof Error ? e.message : 'Erro ao excluir a categoria.')
  }
}
</script>

<template>
  <div v-if="carregando" class="estado-tela">Carregando…</div>

  <template v-else>
    <div class="barra-acoes">
      <span class="contagem">
        {{ categorias.length }}
        {{ categorias.length === 1 ? 'categoria cadastrada' : 'categorias cadastradas' }}
      </span>
      <BaseButton @click="abrir()">+ Nova categoria</BaseButton>
    </div>

    <div v-if="erro" class="alerta-erro">{{ erro }}</div>

    <div class="tabela-scroll">
      <table class="tabela">
        <thead>
          <tr>
            <th>Nome</th>
            <th aria-label="ações"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="categorias.length === 0 && !erro">
            <td colspan="2" class="tabela__vazio">Nenhuma categoria cadastrada.</td>
          </tr>
          <tr v-for="c in categorias" :key="c.id">
            <td>{{ c.nome }}</td>
            <td class="num">
              <button class="link-acao" @click="abrir(c)">Editar</button>
              <button class="link-acao link-acao--perigo" @click="excluir(c)">Excluir</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>

  <BaseModal
    :aberto="modalAberto"
    :titulo="editando ? 'Editar categoria' : 'Nova categoria'"
    @fechar="modalAberto = false"
  >
    <div v-if="erroModal" class="alerta-erro">{{ erroModal }}</div>
    <BaseField rotulo="Nome" obrigatorio>
      <input v-model="nome" type="text" placeholder="Ex: Bebidas" @keyup.enter="salvar" />
    </BaseField>
    <template #acoes>
      <BaseButton variante="fantasma" @click="modalAberto = false">Cancelar</BaseButton>
      <BaseButton :carregando="salvando" @click="salvar">
        {{ salvando ? 'Salvando…' : 'Salvar' }}
      </BaseButton>
    </template>
  </BaseModal>
</template>
