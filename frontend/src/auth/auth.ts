// đăng nhập trên trang của FinQuery (QĐ-045): form gửi email + mật khẩu thẳng tới Keycloak (direct access grant),
// nhận access/refresh token; Keycloak vẫn kiểm mật khẩu, chính sách, chống dò; core chỉ kiểm token.
// không đặt VITE_KEYCLOAK_URL thì tắt đăng nhập (backend chạy finq.security.enabled=false)
const url = import.meta.env.VITE_KEYCLOAK_URL
export const authEnabled = !!url

const realm = import.meta.env.VITE_KEYCLOAK_REALM ?? 'finquery'
const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT ?? 'finquery-web'
const oidc = `${url}/realms/${realm}/protocol/openid-connect`

type Session = { access: string; refresh: string; accessExp: number; refreshExp: number }

// phiên giữ trong sessionStorage: tải lại trang không phải đăng nhập lại, đóng tab là hết
const KEY = 'finq.session'
let session: Session | null = load()
let refreshing: Promise<void> | null = null
const expiredListeners = new Set<() => void>()

function load(): Session | null {
  try {
    const s = sessionStorage.getItem(KEY)
    return s ? (JSON.parse(s) as Session) : null
  } catch {
    return null
  }
}

function save(s: Session | null) {
  session = s
  try {
    if (s) {
      sessionStorage.setItem(KEY, JSON.stringify(s))
    } else {
      sessionStorage.removeItem(KEY)
    }
  } catch {
    // trình duyệt chặn sessionStorage: vẫn chạy, chỉ là tải lại trang phải đăng nhập lại
  }
}

// lỗi của Keycloak thành câu tiếng Việt cho người dùng
function message(body: { error?: string; error_description?: string }): string {
  const d = body.error_description ?? ''
  if (d.includes('Invalid user credentials')) return 'Sai email hoặc mật khẩu.'
  if (d.includes('not fully set up')) return 'Tài khoản cần hoàn tất thiết lập (ví dụ đổi mật khẩu tạm). Liên hệ quản trị viên.'
  if (d.includes('disabled')) return 'Tài khoản đang bị khoá (đăng nhập sai nhiều lần hoặc bị vô hiệu). Thử lại sau hoặc liên hệ quản trị viên.'
  return d || body.error || 'Không đăng nhập được.'
}

async function tokenRequest(params: Record<string, string>): Promise<Session> {
  const res = await fetch(`${oidc}/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ client_id: clientId, ...params }),
  })
  const body = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error(message(body))
  }
  const now = Date.now()
  return {
    access: body.access_token,
    refresh: body.refresh_token,
    accessExp: now + body.expires_in * 1000,
    refreshExp: now + (body.refresh_expires_in ?? 1800) * 1000,
  }
}

export function isLoggedIn(): boolean {
  return !authEnabled || (!!session && session.refreshExp > Date.now())
}

export async function login(email: string, password: string) {
  save(await tokenRequest({ grant_type: 'password', username: email.trim(), password, scope: 'openid' }))
}

// thu hồi phiên ở Keycloak rồi xoá token ở trình duyệt
export async function logout() {
  const s = session
  save(null)
  if (s) {
    await fetch(`${oidc}/logout`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ client_id: clientId, refresh_token: s.refresh }),
    }).catch(() => undefined)
  }
  expiredListeners.forEach(f => f())
}

// giao diện đăng ký để quay về màn hình đăng nhập khi phiên hết hạn
export function onSessionExpired(f: () => void): () => void {
  expiredListeners.add(f)
  return () => expiredListeners.delete(f)
}

// header Authorization cho mọi lần gọi core; token còn dưới 30 s thì gia hạn bằng refresh token (một lần cho cả trang)
export async function authHeaders(): Promise<Record<string, string>> {
  if (!authEnabled) {
    return {}
  }
  if (!session || session.refreshExp <= Date.now()) {
    save(null)
    expiredListeners.forEach(f => f())
    throw new Error('Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại')
  }
  if (session.accessExp - Date.now() < 30_000) {
    refreshing ??= tokenRequest({ grant_type: 'refresh_token', refresh_token: session.refresh })
      .then(save)
      .catch(e => {
        save(null)
        expiredListeners.forEach(f => f())
        throw e
      })
      .finally(() => {
        refreshing = null
      })
    await refreshing
  }
  return { Authorization: `Bearer ${session!.access}` }
}

// xin token mới ngay (sau khi sửa họ tên: token cũ còn tên cũ)
export async function refreshNow() {
  if (authEnabled && session) {
    save(await tokenRequest({ grant_type: 'refresh_token', refresh_token: session.refresh }))
  }
}

// fetch tới core có kèm token
export async function apiFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const headers = { ...((init.headers as Record<string, string>) ?? {}), ...(await authHeaders()) }
  return fetch(input, { ...init, headers })
}
