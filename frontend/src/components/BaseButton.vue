<script setup lang="ts">
withDefaults(
  defineProps<{
    variante?: 'primario' | 'fantasma' | 'perigo'
    tipo?: 'button' | 'submit'
    carregando?: boolean
    desabilitado?: boolean
  }>(),
  { variante: 'primario', tipo: 'button', carregando: false, desabilitado: false },
)
</script>

<template>
  <button
    :type="tipo"
    class="btn"
    :class="`btn--${variante}`"
    :disabled="desabilitado || carregando"
  >
    <span v-if="carregando" class="btn__spinner" aria-hidden="true" />
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 9px 18px;
  border-radius: var(--raio-sm);
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition:
    background var(--transicao),
    border-color var(--transicao),
    opacity var(--transicao);
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn--primario {
  background: var(--cor-primario);
  color: var(--cor-primario-contraste);
}
.btn--primario:not(:disabled):hover {
  background: var(--cor-primario-hover);
}

.btn--fantasma {
  background: transparent;
  border-color: var(--cor-borda-forte);
  color: var(--cor-texto-suave);
}
.btn--fantasma:not(:disabled):hover {
  background: var(--cor-superficie-2);
  color: var(--cor-texto);
}

.btn--perigo {
  background: transparent;
  border-color: var(--cor-erro-borda);
  color: var(--cor-erro);
}
.btn--perigo:not(:disabled):hover {
  background: var(--cor-erro-superficie);
}

.btn__spinner {
  width: 13px;
  height: 13px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: girar 0.7s linear infinite;
}

@keyframes girar {
  to {
    transform: rotate(360deg);
  }
}
</style>
