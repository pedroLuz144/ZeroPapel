import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

// Prefixos de API que, no dev server, são encaminhados para o backend Spring (porta 8080).
// Em produção a SPA é servida pelo próprio Spring, então as chamadas relativas caem no mesmo host.
const PREFIXOS_API = [
  '/auth',
  '/pedidos',
  '/itens',
  '/categorias',
  '/plataformas',
  '/formasDePagamento',
  '/fechamento',
  '/dashboard',
  '/usuarios',
]

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    // O build sai direto na pasta estática do Spring Boot.
    outDir: fileURLToPath(new URL('../src/main/resources/static', import.meta.url)),
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: Object.fromEntries(
      PREFIXOS_API.map((p) => [p, { target: 'http://localhost:8080', changeOrigin: true }]),
    ),
  },
  test: {
    // jsdom para poder montar componentes; os testes ficam ao lado do arquivo testado.
    environment: 'jsdom',
    include: ['src/**/*.spec.ts'],
  },
})
