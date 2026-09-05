<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseField from '@/components/BaseField.vue'
import BaseButton from '@/components/BaseButton.vue'
import { authApi, HttpError } from '@/api'
import { auth } from '@/stores/auth'

const router = useRouter()
const route = useRoute()

const usuario = ref('')
const senha = ref('')
const erro = ref('')
const carregando = ref(false)

async function entrar(): Promise<void> {
  if (!usuario.value.trim() || !senha.value.trim()) {
    erro.value = 'Preencha usuário e senha.'
    return
  }
  carregando.value = true
  erro.value = ''
  try {
    const r = await authApi.login({ usuario: usuario.value.trim(), senha: senha.value })
    auth.definirSessao({
      token: r.token,
      refreshToken: r.refreshToken,
      usuario: usuario.value.trim(),
      cargo: r.cargo,
    })
    const redir = route.query.redir
    router.replace(typeof redir === 'string' ? redir : { name: 'pdv' })
  } catch (e) {
    erro.value =
      e instanceof HttpError && e.status !== 0
        ? e.message
        : 'Erro de conexão com o servidor.'
  } finally {
    carregando.value = false
  }
}
</script>

<template>
  <div class="login">
    <form class="cartao" @submit.prevent="entrar">
      <div class="cartao__marca">
        <span class="cartao__titulo">GOLDEN PETISCARIA</span>
        <span class="cartao__sub">Sistema de Controle de Pedidos</span>
      </div>

      <div v-if="erro" class="alerta-erro">{{ erro }}</div>

      <div class="pilha">
        <BaseField rotulo="Usuário">
          <input v-model="usuario" type="text" autocomplete="username" placeholder="Digite seu usuário" />
        </BaseField>
        <BaseField rotulo="Senha">
          <input
            v-model="senha"
            type="password"
            autocomplete="current-password"
            placeholder="Digite sua senha"
          />
        </BaseField>
      </div>

      <BaseButton tipo="submit" :carregando="carregando" class="login__botao">
        {{ carregando ? 'Entrando…' : 'Entrar no sistema' }}
      </BaseButton>

      <p class="cartao__nota">Acesso restrito. Solicite seu cadastro ao gerente.</p>
    </form>
  </div>
</template>

<style scoped>
.login {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.cartao {
  width: 100%;
  max-width: 400px;
  background: var(--cor-superficie);
  border: 1px solid var(--cor-borda);
  border-radius: var(--raio-lg);
  padding: 38px 36px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.cartao__marca {
  text-align: center;
}

.cartao__titulo {
  display: block;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--cor-acento);
}

.cartao__sub {
  display: block;
  font-size: 12px;
  color: var(--cor-texto-suave);
  margin-top: 6px;
}

.login__botao {
  width: 100%;
}

.cartao__nota {
  text-align: center;
  font-size: 12px;
  color: var(--cor-texto-fraco);
}
</style>
