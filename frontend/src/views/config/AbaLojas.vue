<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseField from '@/components/BaseField.vue'
import BaseModal from '@/components/BaseModal.vue'
import { lojasApi } from '@/api'
import type { LojaResponse } from '@/api/types'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const registros = ref<LojaResponse[]>([])
const carregando = ref(true)
const erro = ref('')

const modalAberto = ref(false)
const editando = ref<LojaResponse | null>(null)
const erroModal = ref('')
const salvando = ref(false)
const form = reactive({ nome: '' })

const contagem = computed(() => {
  const n = registros.value.length
  return `${n} ${n === 1 ? 'loja cadastrada' : 'lojas cadastradas'}`
})

async function carregar(): Promise<void> {
  erro.value = ''
  try {
    registros.value = await lojasApi.listar()
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar as lojas.'
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)

function abrir(registro?: LojaResponse): void {
  editando.value = registro ?? null
  form.nome = registro?.nome ?? ''
  erroModal.value = ''
  modalAberto.value = true
}

async function salvar(): Promise<void> {
  if (!form.nome.trim()) {
    erroModal.value = 'Preencha o nome.'
    return
  }

  salvando.value = true
  erroModal.value = ''
  try {
    const body = { nome: form.nome.trim() }
    if (editando.value) {
      await lojasApi.atualizar(editando.value.id, body)
      toast.sucesso('Loja atualizada.')
    } else {
      await lojasApi.adicionar(body)
      toast.sucesso('Loja adicionada.')
    }
    modalAberto.value = false
    await carregar()
  } catch (e) {
    erroModal.value = e instanceof Error ? e.message : 'Erro ao salvar.'
  } finally {
    salvando.value = false
  }
}

async function alternarAtivo(registro: LojaResponse): Promise<void> {
  try {
    await lojasApi.atualizar(registro.id, { ativo: !registro.ativo })
    toast.sucesso(registro.ativo ? 'Loja desativada.' : 'Loja ativada.')
    await carregar()
  } catch (e) {
    toast.erro(e instanceof Error ? e.message : 'Erro ao atualizar a loja.')
  }
}
</script>

<template>
  <div v-if="carregando" class="estado-tela">Carregando…</div>

  <template v-else>
    <div class="barra-acoes">
      <span class="contagem">{{ contagem }}</span>
      <BaseButton @click="abrir()">+ Nova loja</BaseButton>
    </div>

    <div v-if="erro" class="alerta-erro">{{ erro }}</div>

    <p class="ajuda">
      Toda venda pertence a uma loja. Depois de cadastrar uma loja, defina em
      <strong>Canais de venda</strong> em quais plataformas ela opera e com qual taxa.
    </p>

    <div class="tabela-scroll">
      <table class="tabela">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Situação</th>
            <th aria-label="ações"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="registros.length === 0 && !erro">
            <td colspan="3" class="tabela__vazio">Nenhuma loja cadastrada.</td>
          </tr>
          <tr v-for="r in registros" :key="r.id">
            <td>{{ r.nome }}</td>
            <td>
              <span class="badge" :class="r.ativo ? 'badge--on' : 'badge--off'">
                {{ r.ativo ? 'Ativa' : 'Inativa' }}
              </span>
            </td>
            <td class="num">
              <button class="link-acao" @click="abrir(r)">Renomear</button>
              <button class="link-acao" :class="{ 'link-acao--perigo': r.ativo }" @click="alternarAtivo(r)">
                {{ r.ativo ? 'Desativar' : 'Ativar' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>

  <BaseModal
    :aberto="modalAberto"
    :titulo="editando ? 'Renomear loja' : 'Nova loja'"
    @fechar="modalAberto = false"
  >
    <div v-if="erroModal" class="alerta-erro">{{ erroModal }}</div>

    <div class="pilha">
      <BaseField rotulo="Nome" obrigatorio>
        <input v-model="form.nome" type="text" placeholder="Ex: Unidade Centro" />
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

<style scoped>
.ajuda {
  font-size: 12px;
  color: var(--cor-texto-fraco);
  margin-bottom: 14px;
}
</style>
