import { apiFetch } from '../auth/auth'

// vai trò FinQuery của một tài khoản; pending = chưa có vai trò (chờ duyệt)
export type AccountRole = 'analyst' | 'document_manager' | 'admin' | 'pending'

export type Account = {
  id: string
  email: string
  firstName: string
  lastName: string
  enabled: boolean
  emailVerified: boolean
  createdAt: number
  role: AccountRole
}

export type DocumentItem = {
  taiLieuId: string
  maCongTy: string
  tenHienThi: string
  loai: string
  nam: number | null
  ky: string | null
  phamVi: string | null
  ngayBanHanh: string | null
  tongSoTrang: number
  trangThai: 'cho_ocr' | 'cho_embed' | 'da_embed'
  ngayOcr: string | null
  ngayEmbed: string | null
  soChunk: number | null
}

export type DocumentPage = { items: DocumentItem[]; total: number; page: number; size: number }

async function body<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let message = `Máy chủ trả lỗi ${res.status}`
    try {
      const b = await res.json()
      message = b.message ?? message
    } catch {
      // không phải JSON
    }
    throw new Error(message)
  }
  return res.json() as Promise<T>
}

const json = (method: string, data: unknown): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(data),
})

// --- người dùng (chỉ admin)
export const listAccounts = async (search: string) =>
  body<Account[]>(await apiFetch(`/api/v1/admin/users${search ? `?search=${encodeURIComponent(search)}` : ''}`))

export const createAccount = async (a: { email: string; firstName: string; lastName: string; role: AccountRole }) =>
  body<{ id: string }>(await apiFetch('/api/v1/admin/users', json('POST', a)))

export const setAccountRole = async (id: string, role: AccountRole) =>
  body(await apiFetch(`/api/v1/admin/users/${id}/role`, json('PUT', { role })))

export const setAccountEnabled = async (id: string, enabled: boolean) =>
  body(await apiFetch(`/api/v1/admin/users/${id}/enabled`, json('PUT', { enabled })))

export const sendPasswordReset = async (id: string) =>
  body(await apiFetch(`/api/v1/admin/users/${id}/reset-password`, { method: 'POST' }))

// --- tài liệu (document_manager, admin)
export const listDocuments = async (q: string, loai: string, page: number) =>
  body<DocumentPage>(
    await apiFetch(`/api/v1/documents?${new URLSearchParams({ q, loai, page: String(page), size: '15' })}`),
  )

// multipart: file + metadata; nạp chạy nền, core trả 202 kèm trạng thái
export const uploadDocument = async (file: File, meta: Record<string, string>) => {
  const form = new FormData()
  form.append('file', file)
  Object.entries(meta).forEach(([k, v]) => form.append(k, v))
  return body<DocumentItem>(await apiFetch('/api/v1/documents', { method: 'POST', body: form }))
}

export const reindexDocument = async (id: string) =>
  body(await apiFetch(`/api/v1/documents/${encodeURIComponent(id)}/index`, { method: 'POST' }))

// --- quên mật khẩu (chưa đăng nhập nên không kèm token)
export const forgotPassword = async (email: string) =>
  body<{ message: string }>(await fetch('/api/v1/auth/forgot-password', json('POST', { email })))
