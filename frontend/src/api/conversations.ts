import { apiFetch } from '../auth/auth'
import type { ConversationDetail, ConversationSummary, FeedbackLevel, Me } from './types'

async function json<T>(res: Response): Promise<T> {
  if (!res.ok) {
    // core trả {"code", "message"}: hiện câu thông báo, không hiện nguyên JSON
    const text = await res.text().catch(() => '')
    let message = text
    try {
      message = JSON.parse(text).message ?? text
    } catch {
      // không phải JSON
    }
    throw new Error(message || `Máy chủ trả lỗi ${res.status}`)
  }
  return res.json() as Promise<T>
}

// người đang đăng nhập, vai trò và việc được làm (gọi được cả khi tài khoản chưa được duyệt)
export const me = async () => json<Me>(await apiFetch('/api/v1/me'))

export const listConversations = async () => json<ConversationSummary[]>(await apiFetch('/api/v1/conversations'))

export const getConversation = async (id: number) => json<ConversationDetail>(await apiFetch(`/api/v1/conversations/${id}`))

// đánh giá một câu trả lời; sai (một phần / toàn bộ) phải có lý do
export const sendFeedback = async (messageId: number, level: FeedbackLevel, reason: string) =>
  json<{ messageId: number; level: FeedbackLevel }>(
    await apiFetch('/api/v1/feedback', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ messageId, level, reason }),
    }),
  )

// chỉ hội thoại của mình; xoá kéo theo tin nhắn và đánh giá
export const deleteConversation = async (id: number) =>
  json<{ id: number }>(await apiFetch(`/api/v1/conversations/${id}`, { method: 'DELETE' }))

export const renameConversation = async (id: number, title: string) =>
  json<{ id: number; title: string }>(
    await apiFetch(`/api/v1/conversations/${id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title }),
    }),
  )

// thông tin cá nhân: tự sửa họ tên, đổi mật khẩu (phải đúng mật khẩu hiện tại)
export const updateProfile = async (lastName: string, firstName: string) =>
  json(
    await apiFetch('/api/v1/me/profile', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ lastName, firstName }),
    }),
  )

export const changePassword = async (currentPassword: string, newPassword: string) =>
  json(
    await apiFetch('/api/v1/me/password', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ currentPassword, newPassword }),
    }),
  )
