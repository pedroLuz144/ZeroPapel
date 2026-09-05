/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base das chamadas de API. Vazio = mesmo host (proxy no dev, Spring em produção). */
  readonly VITE_API_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
