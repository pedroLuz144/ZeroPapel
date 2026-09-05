<script setup lang="ts">
defineProps<{
  rotulo?: string
  obrigatorio?: boolean
  erro?: string
  dica?: string
}>()
</script>

<template>
  <label class="campo">
    <span v-if="rotulo" class="campo__rotulo">
      {{ rotulo }}<span v-if="obrigatorio" class="campo__req">*</span>
    </span>
    <slot />
    <span v-if="erro" class="campo__erro">{{ erro }}</span>
    <span v-else-if="dica" class="campo__dica">{{ dica }}</span>
  </label>
</template>

<style scoped>
.campo {
  display: block;
}

.campo__rotulo {
  display: block;
  font-size: 12px;
  color: var(--cor-texto-suave);
  margin-bottom: 6px;
}

.campo__req {
  color: var(--cor-acento);
  margin-left: 3px;
}

.campo__erro {
  display: block;
  font-size: 12px;
  color: var(--cor-erro);
  margin-top: 5px;
}

.campo__dica {
  display: block;
  font-size: 12px;
  color: var(--cor-texto-fraco);
  margin-top: 5px;
}
</style>
