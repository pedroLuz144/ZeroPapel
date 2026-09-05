import { createRouter, createWebHistory, type RouteLocationRaw } from 'vue-router'
import { auth } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    /** Rota acessível sem sessão (ex.: login). */
    publico?: boolean
    /** Rota restrita ao cargo GERENTE. */
    gerente?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { publico: true },
    },
    {
      path: '/app',
      component: () => import('@/views/AppShell.vue'),
      children: [
        { path: '', redirect: { name: 'pdv' } },
        { path: 'pdv', name: 'pdv', component: () => import('@/views/PdvView.vue') },
        { path: 'pedidos', name: 'pedidos', component: () => import('@/views/PedidosView.vue') },
        {
          path: 'financeiro',
          name: 'financeiro',
          component: () => import('@/views/FinanceiroView.vue'),
          meta: { gerente: true },
        },
        {
          path: 'configuracoes',
          name: 'configuracoes',
          component: () => import('@/views/ConfiguracoesView.vue'),
        },
      ],
    },
    { path: '/', redirect: '/app' },
    { path: '/:pathMatch(.*)*', redirect: '/app' },
  ],
})

router.beforeEach((to): boolean | RouteLocationRaw => {
  if (to.meta.publico) {
    return auth.autenticado ? { name: 'pdv' } : true
  }
  if (!auth.autenticado) {
    return { name: 'login', query: { redir: to.fullPath } }
  }
  if (to.meta.gerente && !auth.isGerente) {
    return { name: 'pdv' }
  }
  return true
})

export default router
