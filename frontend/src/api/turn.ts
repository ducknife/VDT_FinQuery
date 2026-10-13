import type { AgentEvent, AskResult, ConversationMessage, Source, Step, Turn, Verification } from './types'

// gộp một sự kiện SSE vào lượt hỏi đáp (trả bản mới, không sửa bản cũ)
export function applyEvent(turn: Turn, e: AgentEvent): Turn {
  const d = e.data
  switch (e.type) {
    case 'step':
      if (d.phase === 'understand') {
        return addStep(turn, { kind: 'Hiểu câu hỏi', text: String(d.normalized) })
      }
      if (d.phase === 'answer') {
        return addStep(turn, { kind: 'Viết', text: `Viết câu trả lời từ ${d.evidence} bằng chứng` })
      }
      if (d.phase === 'history') {
        return addStep(turn, { kind: 'Ngữ cảnh', text: 'Dùng các lượt trước của cuộc hội thoại để hiểu câu hỏi' })
      }
      if (d.phase === 'lessons') {
        return addStep(turn, { kind: 'Ngữ cảnh', text: 'Dùng góp ý từ feedback của câu hỏi tương tự' })
      }
      return turn
    case 'saved':
      return { ...turn, messageId: Number(d.messageId) }
    case 'plan':
      return addStep(turn, { kind: 'Kế hoạch', text: String(d.text) })
    case 'tool':
      return addStep(turn, { kind: 'Công cụ', text: String(d.text) })
    case 'retrieval':
      return addStep(turn, { kind: 'Truy hồi', text: retrievalText(d) })
    case 'sources':
      return addStep(turn, { kind: 'Nguồn', text: 'Các đoạn liên quan nhất', sources: d.items as Source[] })
    case 'tool_result':
      return addStep(turn, { kind: 'Kết quả', text: String(d.text) })
    case 'answer_delta':
      return { ...turn, answer: turn.answer + String(d.text) }
    // có phép tính: chữ đã stream còn chỗ {{K#}}, thay bằng bản đã điền kết quả Java tính
    case 'answer_final':
      return { ...turn, answer: String(d.text) }
    case 'verification':
      return { ...turn, verification: d as unknown as Verification }
    case 'done':
      return { ...turn, result: d.result as AskResult, running: false }
    case 'error':
      return { ...turn, error: String(d.message), running: false }
    default:
      return turn
  }
}

// tin nhắn đã lưu (mở lại hội thoại cũ) thành một lượt: các bước lấy từ dấu vết tool, kết quả kiểm từ AskResult
export function turnFromMessage(m: ConversationMessage): Turn {
  const r = m.result
  return {
    id: m.id,
    question: m.question,
    steps: (r?.toolTrace ?? []).map(t => ({ kind: 'Công cụ', text: t })),
    answer: m.answer ?? r?.answer ?? '',
    verification: r
      ? {
          status: r.status,
          citations: r.citations ?? [],
          unverifiedNumbers: r.unverifiedNumbers ?? [],
          unknownIds: r.unknownIds ?? [],
          missing: r.missing ?? '',
        }
      : undefined,
    result: r ?? undefined,
    running: false,
    messageId: m.id,
    feedback: m.feedback,
  }
}

function addStep(turn: Turn, step: Step): Turn {
  return { ...turn, steps: [...turn.steps, step] }
}

function retrievalText(d: Record<string, unknown>): string {
  switch (d.step) {
    case 'preprocess':
      return `Chuẩn hoá câu tìm · ${d.filter}`
    case 'search':
      return `Tìm trong kho: ${d.found} đoạn (lọc ${d.filterMode}), bỏ ${d.englishDropped} đoạn tiếng Anh, còn ${d.kept}`
    case 'rerank_start':
      return `Xếp hạng ${d.candidates} đoạn bằng 2 tầng…`
    case 'rerank':
      return `Xếp hạng xong (${(d.tiers as string[]).join('; ')}) → ${d.kept} đoạn`
    default:
      return String(d.step)
  }
}
