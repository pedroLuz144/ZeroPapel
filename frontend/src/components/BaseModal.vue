<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'

const props = defineProps<{
  aberto: boolean
  titulo?: string
  largo?: boolean
}>()

const emit = defineEmits<{ fechar: [] }>()

function aoTeclar(e: KeyboardEvent): void {
  if (e.key === 'Escape') emit('fechar')
}

watch(
  () => props.aberto,
  (aberto) => {
    if (aberto) {
      document.addEventListener('keydown', aoTeclar)
      document.body.style.overflow = 'hidden'
    } else {
      document.removeEventListener('keydown', aoTeclar)
      document.body.style.overflow = ''
    }
  },
)

onBeforeUnmount(() => {
  document.removeEventListener('keydown', aoTeclar)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="aberto" class="modal-fundo" @mousedown.self="emit('fechar')">
        <div class="modal" :class="{ 'modal--largo': largo }" role="dialog" aria-modal="true">
          <h2 v-if="titulo" class="modal__titulo">{{ titulo }}</h2>
          <slot />
          <div v-if="$slots.acoes" class="modal__acoes">
            <slot name="acoes" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-fundo {
  position: fixed;
  inset: 0;
  background: rgba(20, 18, 14, 0.4);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 200;
}

.modal {
  background: var(--cor-superficie);
  border: 1px solid var(--cor-borda);
  border-radius: var(--raio-lg);
  box-shadow: var(--sombra-2);
  width: 100%;
  max-width: 440px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 26px 28px;
}

.modal--largo {
  max-width: 620px;
}

.modal__titulo {
  font-size: 15px;
  margin-bottom: 20px;
}

.modal__acoes {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 24px;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity var(--transicao);
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-active .modal,
.modal-leave-active .modal {
  transition: transform var(--transicao);
}
.modal-enter-from .modal,
.modal-leave-to .modal {
  transform: translateY(8px);
}
</style>
