<script setup lang="ts">
import { ref } from 'vue'
import { auth } from '@/stores/auth'
import AbaCardapio from '@/views/config/AbaCardapio.vue'
import AbaCategorias from '@/views/config/AbaCategorias.vue'
import AbaPlataformas from '@/views/config/AbaPlataformas.vue'
import AbaLojas from '@/views/config/AbaLojas.vue'
import AbaCanaisDeVenda from '@/views/config/AbaCanaisDeVenda.vue'
import AbaFormasPagamento from '@/views/config/AbaFormasPagamento.vue'
import AbaUsuarios from '@/views/config/AbaUsuarios.vue'

type Aba = 'cardapio' | 'categorias' | 'plataformas' | 'lojas' | 'canais' | 'formas' | 'usuarios'

const aba = ref<Aba>('cardapio')

const abas: { id: Aba; rotulo: string; gerente?: boolean }[] = [
  { id: 'cardapio', rotulo: 'Cardápio' },
  { id: 'categorias', rotulo: 'Categorias', gerente: true },
  { id: 'plataformas', rotulo: 'Plataformas', gerente: true },
  { id: 'lojas', rotulo: 'Lojas', gerente: true },
  { id: 'canais', rotulo: 'Canais de venda', gerente: true },
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
  <AbaLojas v-else-if="aba === 'lojas'" />
  <AbaCanaisDeVenda v-else-if="aba === 'canais'" />
  <AbaFormasPagamento v-else-if="aba === 'formas'" />
  <AbaUsuarios v-else-if="aba === 'usuarios'" />
</template>
