<script setup lang="ts">
import { ref } from 'vue'
import { auth } from '@/stores/auth'
import AbaCardapio from './config/AbaCardapio.vue'
import AbaCategorias from './config/AbaCategorias.vue'
import AbaPlataformas from './config/AbaPlataformas.vue'
import AbaFormasPagamento from './config/AbaFormasPagamento.vue'
import AbaUsuarios from './config/AbaUsuarios.vue'

type Aba = 'cardapio' | 'categorias' | 'plataformas' | 'formas' | 'usuarios'

const aba = ref<Aba>('cardapio')

const abas: { id: Aba; rotulo: string; gerente?: boolean }[] = [
  { id: 'cardapio', rotulo: 'Cardápio' },
  { id: 'categorias', rotulo: 'Categorias', gerente: true },
  { id: 'plataformas', rotulo: 'Plataformas', gerente: true },
  { id: 'formas', rotulo: 'Formas de Pagamento', gerente: true },
  { id: 'usuarios', rotulo: 'Usuários', gerente: true },
]
</script>

<template>
  <div class="tabs">
    <template v-for="a in abas" :key="a.id">
      <button
        v-if="!a.gerente || auth.isGerente"
        class="tab"
        :class="{ 'tab--ativo': aba === a.id }"
        @click="aba = a.id"
      >
        {{ a.rotulo }}
      </button>
    </template>
  </div>

  <AbaCardapio v-if="aba === 'cardapio'" />
  <AbaCategorias v-else-if="aba === 'categorias'" />
  <AbaPlataformas v-else-if="aba === 'plataformas'" />
  <AbaFormasPagamento v-else-if="aba === 'formas'" />
  <AbaUsuarios v-else-if="aba === 'usuarios'" />
</template>
