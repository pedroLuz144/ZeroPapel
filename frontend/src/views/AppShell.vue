<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import BaseButton from '@/components/BaseButton.vue'
import { auth } from '@/stores/auth'
import { authApi } from '@/api'

const route = useRoute()
const router = useRouter()

const TITULOS: Record<string, string> = {
  pdv: 'PDV - Balcão',
  pedidos: 'Painel de Pedidos',
  financeiro: 'Dashboard Financeiro',
  configuracoes: 'Configurações',
}

const titulo = computed(() => TITULOS[String(route.name)] ?? '')

async function sair(): Promise<void> {
  const rt = auth.estado.refreshToken
  if (rt) await authApi.logout(rt).catch(() => {})
  auth.limpar()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="shell">
    <AppSidebar />
    <div class="shell__main">
      <header class="shell__topo">
        <h1 class="shell__titulo">{{ titulo }}</h1>
        <div class="shell__usuario">
          <span class="shell__nome">{{ auth.estado.usuario }}</span>
          <span class="shell__cargo">{{ auth.isGerente ? 'Gerente' : 'Operador' }}</span>
          <BaseButton variante="fantasma" @click="sair">Sair</BaseButton>
        </div>
      </header>
      <main class="shell__conteudo">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.shell__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.shell__topo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 26px;
  border-bottom: 1px solid var(--cor-borda);
}

.shell__titulo {
  font-size: 15px;
}

.shell__usuario {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--cor-texto-suave);
}

.shell__cargo {
  border: 1px solid var(--cor-borda-forte);
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 11px;
  color: var(--cor-texto-fraco);
}

.shell__conteudo {
  flex: 1;
  overflow-y: auto;
  padding: 24px 26px;
}
</style>
