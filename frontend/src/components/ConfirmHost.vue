<script setup lang="ts">
import BaseModal from './BaseModal.vue'
import BaseButton from './BaseButton.vue'
import { useConfirm } from '@/composables/useConfirm'

const { estado, responder } = useConfirm()
</script>

<template>
  <BaseModal :aberto="estado.aberto" :titulo="estado.titulo" @fechar="responder(false)">
    <p class="confirm__msg">{{ estado.mensagem }}</p>
    <template #acoes>
      <BaseButton variante="fantasma" @click="responder(false)">Cancelar</BaseButton>
      <BaseButton variante="perigo" @click="responder(true)">{{ estado.rotuloConfirmar }}</BaseButton>
    </template>
  </BaseModal>
</template>

<style scoped>
.confirm__msg {
  font-size: 13px;
  color: var(--cor-texto-suave);
}
</style>
