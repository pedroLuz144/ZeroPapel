import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

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
  '/lojas',
  '/canaisDeVenda',
]

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
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
    environment: 'jsdom',
    include: ['src/**/*.spec.ts'],
  },
})
