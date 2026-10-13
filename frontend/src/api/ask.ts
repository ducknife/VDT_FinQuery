import { apiFetch, authHeaders } from '../auth/auth'
import type { AgentEvent } from './types'

// POST /api/v1/ask trả SSE; EventSource chỉ hỗ trợ GET (và không gắn được token) nên đọc stream bằng fetch và tự tách sự kiện.
// conversationId: hỏi tiếp trong hội thoại cũ (core gửi kèm lịch sử đã compaction); null = hội thoại mới
export async function ask(
  question: string,
  conversationId: number | null,
  onEvent: (e: AgentEvent) => void,
  signal?: AbortSignal,
) {
  const res = await fetch('/api/v1/ask', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream', ...(await authHeaders()) },
    body: JSON.stringify({ question, conversationId }),
    signal,
  })
  if (!res.ok || !res.body) {
    const text = await res.text().catch(() => '')
    throw new Error(`Máy chủ trả lỗi ${res.status}: ${text}`)
  }
  const reader = res.body.pipeThrough(new TextDecoderStream()).getReader()
  let buffer = ''
  for (;;) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }
    buffer += value.replace(/\r\n/g, '\n')
    // mỗi sự kiện kết thúc bằng một dòng trống
    let end = buffer.indexOf('\n\n')
    while (end >= 0) {
      const block = buffer.slice(0, end)
      buffer = buffer.slice(end + 2)
      const event = parse(block)
      if (event) {
        onEvent(event)
      }
      end = buffer.indexOf('\n\n')
    }
  }
}

function parse(block: string): AgentEvent | null {
  let type = 'message'
  const data: string[] = []
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) {
      type = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      data.push(line.slice(5).replace(/^ /, ''))
    }
  }
  if (data.length === 0) {
    return null
  }
  try {
    return { type, data: JSON.parse(data.join('\n')) }
  } catch {
    return { type, data: { text: data.join('\n') } }
  }
}

const pagePath = (taiLieuId: string, trang: number) =>
  `/api/v1/documents/${encodeURIComponent(taiLieuId)}/pages/${trang}`

// ảnh trang cần token nên không dùng <img src> trực tiếp: tải về thành blob, trả URL tạm (nhớ revoke)
export async function pageImage(taiLieuId: string, trang: number): Promise<string> {
  const res = await apiFetch(`${pagePath(taiLieuId, trang)}/image`)
  if (!res.ok) {
    throw new Error(`Không tải được ảnh trang (${res.status})`)
  }
  return URL.createObjectURL(await res.blob())
}

export async function pageText(taiLieuId: string, trang: number): Promise<string> {
  const res = await apiFetch(`${pagePath(taiLieuId, trang)}/text`)
  if (!res.ok) {
    throw new Error(`Không đọc được chữ của trang (${res.status})`)
  }
  return res.text()
}
