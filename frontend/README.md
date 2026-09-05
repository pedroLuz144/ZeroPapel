# ZeroPapel — Frontend

SPA em **Vite + Vue 3 + TypeScript** que consome a API Spring Boot do projeto.

## Pré-requisitos

- Node.js 20+ e npm

## Instalação

```bash
cd frontend
npm install
```

## Desenvolvimento

Suba o backend (`./mvnw spring-boot:run`, porta 8080) e, em outro terminal:

```bash
cd frontend
npm run dev
```

Abre em `http://localhost:5173`. O dev server faz proxy das rotas de API
(`/auth`, `/pedidos`, `/itens`, `/categorias`, `/plataformas`, `/formasDePagamento`,
`/fechamento`, `/usuarios`) para `http://localhost:8080` — sem CORS.

## Build de produção

```bash
npm run build
```

Gera os arquivos em `../src/main/resources/static/` (a pasta é **limpa** a cada build).
Depois o Spring serve tudo em `/`, e o `SpaForwardController` encaminha as rotas de
tela (`/login`, `/app/**`) para o `index.html`.

> A saída do build **não é versionada** (ver `.gitignore` na raiz). Rode `npm run build`
> antes de `./mvnw package` quando quiser um `.jar` com a interface embutida.

## Type-check

```bash
npm run type-check
```

## Estrutura

```
src/
  api/          # camada HTTP tipada
    http.ts       → wrapper de fetch (Bearer token + refresh automático em 401)
    types.ts      → interfaces espelhando os DTOs do backend
    index.ts      → funções por recurso (authApi, pedidosApi, ...)
  stores/
    auth.ts       → sessão (token/refresh/usuário/cargo) persistida em localStorage
  router/         # Vue Router em history mode + guard de autenticação
  composables/
    useToast.ts   → notificações (substitui alert)
    useConfirm.ts → diálogo de confirmação (substitui confirm)
  components/     # BaseButton, BaseField, BaseModal, ToastHost, ConfirmHost, AppSidebar
  views/          # LoginView, AppShell, PdvView, EmBreveView
  styles/
    tokens.css    → design tokens (paleta preto + dourado)
    base.css      → reset e estilos de formulário
  utils/formato.ts
```

## Status da migração

| Tela                  | Rota                  | Situação    |
| --------------------- | --------------------- | ----------- |
| Login                 | `/login`              | ✅ migrada  |
| PDV - Balcão          | `/app/pdv`            | ✅ migrada  |
| Painel de Pedidos     | `/app/pedidos`        | ✅ migrada  |
| Dashboard Financeiro  | `/app/financeiro`     | ✅ migrada  |
| Configurações         | `/app/configuracoes`  | ✅ migrada  |

Todas as telas do sistema antigo (`dashboard.html`) foram portadas.
A aba **Integrações** de Configurações foi deixada de fora — entra junto com a
integração iFood.

`Configurações` é composta por abas em `views/config/` (`AbaCardapio`, `AbaCategorias`,
`AbaTaxas` — reusada para plataformas e formas de pagamento —, `AbaUsuarios`).
