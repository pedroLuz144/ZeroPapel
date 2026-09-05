<script setup lang="ts">
import { auth } from '@/stores/auth'

const itens = [
  { nome: 'PDV - Balcão', para: '/app/pdv' },
  { nome: 'Painel de Pedidos', para: '/app/pedidos' },
  { nome: 'Dashboard Financeiro', para: '/app/financeiro', gerente: true },
  { nome: 'Configurações', para: '/app/configuracoes' },
]
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar__marca">
      <span class="sidebar__nome">GOLDEN PETISCARIA</span>
      <span class="sidebar__sub">Sistema de Pedidos</span>
    </div>

    <nav class="sidebar__nav">
      <span class="sidebar__secao">Menu</span>
      <template v-for="item in itens" :key="item.para">
        <RouterLink
          v-if="!item.gerente || auth.isGerente"
          :to="item.para"
          class="sidebar__item"
          active-class="sidebar__item--ativo"
        >
          {{ item.nome }}
        </RouterLink>
      </template>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  width: var(--largura-sidebar);
  flex-shrink: 0;
  background: var(--cor-superficie);
  border-right: 1px solid var(--cor-borda);
  display: flex;
  flex-direction: column;
}

.sidebar__marca {
  padding: 20px 18px 16px;
  border-bottom: 1px solid var(--cor-borda);
}

.sidebar__nome {
  display: block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--cor-acento);
}

.sidebar__sub {
  display: block;
  font-size: 11px;
  color: var(--cor-texto-fraco);
  margin-top: 3px;
}

.sidebar__nav {
  padding: 10px 0;
}

.sidebar__secao {
  display: block;
  font-size: 10px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--cor-texto-fraco);
  padding: 12px 18px 8px;
}

.sidebar__item {
  display: block;
  padding: 10px 18px;
  font-size: 13px;
  color: var(--cor-texto-suave);
  border-left: 2px solid transparent;
  transition:
    background var(--transicao),
    color var(--transicao);
}

.sidebar__item:hover {
  background: var(--cor-superficie-2);
  color: var(--cor-texto);
}

.sidebar__item--ativo {
  color: var(--cor-texto);
  background: var(--cor-superficie-2);
  border-left-color: var(--cor-acento);
  font-weight: 600;
}
</style>
