<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseField from '@/components/BaseField.vue'
import BaseModal from '@/components/BaseModal.vue'
import { canaisDeVendaApi, lojasApi, plataformasApi } from '@/api'
import type { CanalDeVendaResponse, LojaResponse, PlataformaResponse } from '@/api/types'
import { percentual } from '@/utils/formato'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

interface FormCanal {
  lojaId: number | null
  plataformaId: number | null
  taxaPercentual: string
  merchantId: string
  ativo: boolean
}

const toast = useToast()
const { confirmar } = useConfirm()

const registros = ref<CanalDeVendaResponse[]>([])
const lojas = ref<LojaResponse[]>([])
const plataformas = ref<PlataformaResponse[]>([])
const carregando = ref(true)
const erro = ref('')

const modalAberto = ref(false)
const editando = ref<CanalDeVendaResponse | null>(null)
const erroModal = ref('')
const salvando = ref(false)
const form = reactive<FormCanal>({
  lojaId: null,
  plataformaId: null,
  taxaPercentual: '',
  merchantId: '',
  ativo: true,
})

const contagem = computed(() => {
  const n = registros.value.length
  return `${n} ${n === 1 ? 'canal cadastrado' : 'canais cadastrados'}`
})

async function carregar(): Promise<void> {
  erro.value = ''
  try {
    const [canais, l, p] = await Promise.all([
      canaisDeVendaApi.listar(),
      lojasApi.listar(),
      plataformasApi.listar(),
    ])
    registros.value = canais
    lojas.value = l
    plataformas.value = p
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar os canais de venda.'
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)

function abrir(registro?: CanalDeVendaResponse): void {
  editando.value = registro ?? null
  Object.assign(form, {
    lojaId: registro?.lojaId ?? null,
    plataformaId: registro?.plataformaId ?? null,
    taxaPercentual: registro ? String(registro.taxaPercentual) : '',
    merchantId: registro?.merchantId ?? '',
    ativo: registro ? registro.ativo : true,
  })
  erroModal.value = ''
  modalAberto.value = true
}

async function salvar(): Promise<void> {
  const taxa = Number.parseFloat(form.taxaPercentual)
  if (form.lojaId == null || form.plataformaId == null || !Number.isFinite(taxa)) {
    erroModal.value = 'Selecione a loja e a plataforma e preencha a taxa percentual.'
    return
  }
  if (taxa < 0 || taxa > 100) {
    erroModal.value = 'A taxa precisa ficar entre 0 e 100.'
    return
  }

  salvando.value = true
  erroModal.value = ''
  try {
    const merchantId = form.merchantId.trim()
    if (editando.value) {
      await canaisDeVendaApi.atualizar(editando.value.id, {
        taxaPercentual: taxa,
        merchantId,
        ativo: form.ativo,
      })
      toast.sucesso('Canal atualizado.')
    } else {
      await canaisDeVendaApi.adicionar({
        lojaId: form.lojaId,
        plataformaId: form.plataformaId,
        taxaPercentual: taxa,
        merchantId: merchantId || null,
      })
      toast.sucesso('Canal adicionado.')
    }
    modalAberto.value = false
    await carregar()
  } catch (e) {
    erroModal.value = e instanceof Error ? e.message : 'Erro ao salvar.'
  } finally {
    salvando.value = false
  }
}

async function excluir(registro: CanalDeVendaResponse): Promise<void> {
  const ok = await confirmar(
    `Excluir o canal ${registro.plataformaNome} da loja "${registro.lojaNome}"? Os pedidos já registrados mantêm a taxa da época.`,
    { titulo: 'Excluir', rotuloConfirmar: 'Excluir' },
  )
  if (!ok) return
  try {
    await canaisDeVendaApi.excluir(registro.id)
    toast.sucesso('Canal excluído.')
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
      <BaseButton @click="abrir()">+ Novo canal</BaseButton>
    </div>

    <div v-if="erro" class="alerta-erro">{{ erro }}</div>

    <p class="ajuda">
      Um canal é a operação de uma loja numa plataforma, com a taxa daquele contrato. A taxa vale
      só para vendas novas. O <em>merchant ID</em> é o identificador da loja na plataforma externa
      (iFood) e fica em branco no Balcão.
    </p>

    <div class="tabela-scroll">
      <table class="tabela">
        <thead>
          <tr>
            <th>Loja</th>
            <th>Plataforma</th>
            <th class="num">Taxa</th>
            <th>Merchant ID</th>
            <th>Situação</th>
            <th aria-label="ações"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="registros.length === 0 && !erro">
            <td colspan="6" class="tabela__vazio">Nenhum canal de venda cadastrado.</td>
          </tr>
          <tr v-for="r in registros" :key="r.id">
            <td>{{ r.lojaNome }}</td>
            <td>{{ r.plataformaNome }}</td>
            <td class="num">{{ percentual(r.taxaPercentual) }}</td>
            <td>{{ r.merchantId ?? '—' }}</td>
            <td>
              <span class="badge" :class="r.ativo ? 'badge--on' : 'badge--off'">
                {{ r.ativo ? 'Ativo' : 'Inativo' }}
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
    :titulo="editando ? 'Editar canal de venda' : 'Novo canal de venda'"
    @fechar="modalAberto = false"
  >
    <div v-if="erroModal" class="alerta-erro">{{ erroModal }}</div>

    <div class="pilha">
      <BaseField rotulo="Loja" obrigatorio>
        <select v-model.number="form.lojaId" :disabled="!!editando">
          <option :value="null">Selecione…</option>
          <option v-for="l in lojas" :key="l.id" :value="l.id">{{ l.nome }}</option>
        </select>
      </BaseField>
      <BaseField rotulo="Plataforma" obrigatorio>
        <select v-model.number="form.plataformaId" :disabled="!!editando">
          <option :value="null">Selecione…</option>
          <option v-for="p in plataformas" :key="p.id" :value="p.id">{{ p.nome }}</option>
        </select>
      </BaseField>
      <BaseField rotulo="Taxa percentual (%)" obrigatorio>
        <input
          v-model="form.taxaPercentual"
          type="number"
          step="0.01"
          min="0"
          max="100"
          placeholder="Ex: 12.00"
        />
      </BaseField>
      <BaseField rotulo="Merchant ID" dica="Opcional">
        <input v-model="form.merchantId" type="text" placeholder="ID da loja no iFood" />
      </BaseField>
      <label v-if="editando" class="check">
        <input v-model="form.ativo" type="checkbox" class="check__box" />
        <span>Canal ativo (aceita pedidos novos)</span>
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
