<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseField from '@/components/BaseField.vue'
import BaseModal from '@/components/BaseModal.vue'
import { usuariosApi } from '@/api'
import type { AtualizarUsuarioRequest, Cargo, UsuarioResponse } from '@/api/types'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'

interface FormUsuario {
  nome: string
  usuario: string
  senha: string
  cargo: Cargo | ''
}

const toast = useToast()
const { confirmar } = useConfirm()

const usuarios = ref<UsuarioResponse[]>([])
const carregando = ref(true)
const erro = ref('')

const modalAberto = ref(false)
const editando = ref<UsuarioResponse | null>(null)
const erroModal = ref('')
const salvando = ref(false)
const form = reactive<FormUsuario>({ nome: '', usuario: '', senha: '', cargo: '' })

async function carregar(): Promise<void> {
  erro.value = ''
  try {
    usuarios.value = await usuariosApi.listar()
  } catch (e) {
    erro.value = e instanceof Error ? e.message : 'Erro ao carregar os usuários.'
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)

function abrirNovo(): void {
  editando.value = null
  Object.assign(form, { nome: '', usuario: '', senha: '', cargo: '' })
  erroModal.value = ''
  modalAberto.value = true
}

function abrirEdicao(u: UsuarioResponse): void {
  editando.value = u
  Object.assign(form, { nome: u.nome, usuario: u.usuario, senha: '', cargo: u.cargo })
  erroModal.value = ''
  modalAberto.value = true
}

async function salvar(): Promise<void> {
  salvando.value = true
  erroModal.value = ''
  try {
    if (editando.value) {
      const patch: AtualizarUsuarioRequest = {}
      if (form.nome.trim()) patch.nome = form.nome.trim()
      if (form.usuario.trim()) patch.usuario = form.usuario.trim()
      if (form.cargo) patch.cargo = form.cargo
      if (form.senha) patch.senha = form.senha
      await usuariosApi.atualizar(editando.value.id, patch)
      toast.sucesso('Usuário atualizado.')
    } else {
      if (!form.nome.trim() || !form.usuario.trim() || !form.senha || !form.cargo) {
        erroModal.value = 'Preencha todos os campos.'
        return
      }
      await usuariosApi.cadastrar({
        nome: form.nome.trim(),
        usuario: form.usuario.trim(),
        senha: form.senha,
        cargo: form.cargo,
      })
      toast.sucesso('Usuário cadastrado.')
    }
    modalAberto.value = false
    await carregar()
  } catch (e) {
    erroModal.value = e instanceof Error ? e.message : 'Erro ao salvar o usuário.'
  } finally {
    salvando.value = false
  }
}

async function alternarAtivo(u: UsuarioResponse): Promise<void> {
  const acao = u.ativo ? 'Desativar' : 'Ativar'
  const ok = await confirmar(`${acao} o usuário "${u.usuario}"?`, {
    titulo: `${acao} usuário`,
    rotuloConfirmar: acao,
  })
  if (!ok) return
  try {
    await (u.ativo ? usuariosApi.desativar(u.id) : usuariosApi.ativar(u.id))
    toast.sucesso(`Usuário ${u.ativo ? 'desativado' : 'ativado'}.`)
    await carregar()
  } catch (e) {
    toast.erro(e instanceof Error ? e.message : 'Erro ao alterar o status do usuário.')
  }
}
</script>

<template>
  <div v-if="carregando" class="estado-tela">Carregando…</div>

  <template v-else>
    <div class="barra-acoes">
      <span class="contagem">
        {{ usuarios.length }}
        {{ usuarios.length === 1 ? 'usuário cadastrado' : 'usuários cadastrados' }}
      </span>
      <BaseButton @click="abrirNovo">+ Novo usuário</BaseButton>
    </div>

    <div v-if="erro" class="alerta-erro">{{ erro }}</div>

    <div class="tabela-scroll">
      <table class="tabela">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Login</th>
            <th>Cargo</th>
            <th>Status</th>
            <th aria-label="ações"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="usuarios.length === 0 && !erro">
            <td colspan="5" class="tabela__vazio">Nenhum usuário cadastrado.</td>
          </tr>
          <tr v-for="u in usuarios" :key="u.id">
            <td>{{ u.nome }}</td>
            <td>{{ u.usuario }}</td>
            <td>{{ u.cargo === 'GERENTE' ? 'Gerente' : 'Operador' }}</td>
            <td>
              <span class="badge" :class="u.ativo ? 'badge--on' : 'badge--off'">
                {{ u.ativo ? 'Ativo' : 'Inativo' }}
              </span>
            </td>
            <td class="num">
              <button class="link-acao" @click="abrirEdicao(u)">Editar</button>
              <button
                class="link-acao"
                :class="{ 'link-acao--perigo': u.ativo }"
                @click="alternarAtivo(u)"
              >
                {{ u.ativo ? 'Desativar' : 'Ativar' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>

  <BaseModal
    :aberto="modalAberto"
    :titulo="editando ? 'Editar usuário' : 'Novo usuário'"
    @fechar="modalAberto = false"
  >
    <div v-if="erroModal" class="alerta-erro">{{ erroModal }}</div>

    <div class="pilha">
      <BaseField rotulo="Nome completo" :obrigatorio="!editando">
        <input v-model="form.nome" type="text" placeholder="Ex: Carlos Mendes" />
      </BaseField>
      <BaseField rotulo="Login" :obrigatorio="!editando">
        <input v-model="form.usuario" type="text" placeholder="Ex: carlos" />
      </BaseField>
      <BaseField
        :rotulo="editando ? 'Nova senha' : 'Senha'"
        :obrigatorio="!editando"
        :dica="editando ? 'Deixe em branco para não alterar' : 'Mínimo 4 caracteres'"
      >
        <input v-model="form.senha" type="password" />
      </BaseField>
      <BaseField rotulo="Cargo" :obrigatorio="!editando">
        <select v-model="form.cargo">
          <option value="">Selecione…</option>
          <option value="GERENTE">Gerente</option>
          <option value="OPERADOR">Operador</option>
        </select>
      </BaseField>
    </div>

    <template #acoes>
      <BaseButton variante="fantasma" @click="modalAberto = false">Cancelar</BaseButton>
      <BaseButton :carregando="salvando" @click="salvar">
        {{ salvando ? 'Salvando…' : editando ? 'Salvar' : 'Cadastrar' }}
      </BaseButton>
    </template>
  </BaseModal>
</template>
