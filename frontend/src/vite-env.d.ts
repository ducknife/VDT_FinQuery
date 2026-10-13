/// <reference types="vite/client" />

// biến môi trường của frontend (.env.development); không chứa bí mật
interface ImportMetaEnv {
  readonly VITE_KEYCLOAK_URL?: string
  readonly VITE_KEYCLOAK_REALM?: string
  readonly VITE_KEYCLOAK_CLIENT?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
