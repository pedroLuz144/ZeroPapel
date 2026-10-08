<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseField from '@/components/BaseField.vue'
import BaseModal from '@/components/BaseModal.vue'
import { plataformasApi } from '@/api'
import type { PlataformaResponse } from '@/api/types'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

const toast = useToast()
const { confirmar } = useConfirm()

const registros = ref<PlataformaResponse[]>([])
const carregando = ref(true)
const erro = ref('')

const modalAberto = ref(false)
const editando = ref<PlataformaResponse | null>(null)
const erroModal = ref('')
const salvando = ref(false)
const form = reactive({ nome: '', entrega: true })

const contagem = computed(() => {
  const n = registros.value.length
  return `${n} ${n === 1 ? 'plataforma cadastrada' : 'plataformas cadastradas'}`
})

async function carregar(): Promise<void> {
  erro.value = ''
  try {
    registros.value = await plataformasApi.listar()
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar as plataformas.'
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)

function abrir(registro?: PlataformaResponse): void {
  editando.value = registro ?? null
  Object.assign(form, {
    nome: registro?.nome ?? '',
    entrega: registro ? registro.entrega : true,
  })
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
    const body = { nome: form.nome.trim(), entrega: form.entrega }
    if (editando.value) {
      await plataformasApi.atualizar(editando.value.id, body)
      toast.sucesso('Plataforma atualizada.')
    } else {
      await plataformasApi.adicionar(body)
      toast.sucesso('Plataforma adicionada.')
    }
    modalAberto.value = false
    await carregar()
  } catch (e) {
    erroModal.value = e instanceof Error ? e.message : 'Erro ao salvar.'
  } finally {
    salvando.value = false
  }
}

async function excluir(registro: PlataformaResponse): Promise<void> {
  const ok = await confirmar(
    `Excluir a plataforma "${registro.nome}"? Os canais de venda dela (taxa e merchant ID de cada loja) são excluídos junto.`,
    {
      titulo: 'Excluir',
      rotuloConfirmar: 'Excluir',
    },
  )
  if (!ok) return
  try {
    await plataformasApi.excluir(registro.id)
    toast.sucesso('Plataforma excluída.')
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
      <BaseButton @click="abrir()">+ Nova plataforma</BaseButton>
    </div>

    <div v-if="erro" class="alerta-erro">{{ erro }}</div>

    <p class="ajuda">
      Marque "Canal de entrega" para iFood, Anota Aí e similares (têm etapa <em>Em rota</em>).
      Deixe desmarcado no <strong>Balcão</strong> — é a plataforma que o PDV usa. A taxa de cada
      plataforma é definida por loja, na aba <strong>Canais de venda</strong>.
    </p>

    <div class="tabela-scroll">
      <table class="tabela">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Tipo</th>
            <th aria-label="ações"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="registros.length === 0 && !erro">
            <td colspan="3" class="tabela__vazio">Nenhuma plataforma cadastrada.</td>
          </tr>
          <tr v-for="r in registros" :key="r.id">
            <td>{{ r.nome }}</td>
            <td>
              <span class="badge" :class="r.entrega ? 'badge--on' : ''">
                {{ r.entrega ? 'Entrega' : 'Balcão' }}
              </span>
            </td>
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
    :titulo="editando ? 'Editar plataforma' : 'Nova plataforma'"
    @fechar="modalAberto = false"
  >
    <div v-if="erroModal" class="alerta-erro">{{ erroModal }}</div>

    <div class="pilha">
      <BaseField rotulo="Nome" obrigatorio>
        <input v-model="form.nome" type="text" placeholder="Ex: iFood" />
      </BaseField>
      <label class="check">
        <input v-model="form.entrega" type="checkbox" class="check__box" />
        <span>Canal de entrega (tem etapa "Em rota")</span>
      </label>
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

.check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--cor-texto-suave);
  cursor: pointer;
}

.check__box {
  width: auto;
  margin: 0;
  accent-color: var(--cor-acento);
}
</style>
