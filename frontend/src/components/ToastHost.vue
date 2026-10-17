<script setup lang="ts">
import { useToast } from '@/composables/useToast'

const { fila, fechar } = useToast()
</script>

<template>
  <Teleport to="body">
    <div class="toasts">
      <TransitionGroup name="toast">
        <div
          v-for="t in fila"
          :key="t.id"
          class="toast"
          :class="`toast--${t.tipo}`"
          role="status"
          @click="fechar(t.id)"
        >
          {{ t.texto }}
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toasts {
  position: fixed;
  right: 20px;
  bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 300;
  max-width: 360px;
}

.toast {
  padding: 12px 15px;
  border-radius: var(--raio);
  font-size: 13px;
  cursor: pointer;
  background: var(--cor-superficie);
  border: 1px solid var(--cor-borda-forte);
  border-left-width: 3px;
  color: var(--cor-texto);
  box-shadow: var(--sombra-2);
}

.toast--ok {
  border-color: var(--cor-ok-borda);
}
.toast--erro {
  border-color: var(--cor-erro-borda);
  color: var(--cor-erro);
}

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity var(--transicao),
    transform var(--transicao);
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(12px);
}
</style>
