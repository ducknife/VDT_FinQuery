import { useState } from 'react'
import { sendFeedback } from '../api/conversations'
import type { FeedbackLevel } from '../api/types'

const LEVELS: { level: FeedbackLevel; label: string }[] = [
  { level: 'dung', label: 'Đúng' },
  { level: 'sai_mot_phan', label: 'Sai một phần' },
  { level: 'sai_toan_bo', label: 'Sai toàn bộ' },
]

const LABEL: Record<FeedbackLevel, string> = { dung: 'đúng', sai_mot_phan: 'sai một phần', sai_toan_bo: 'sai toàn bộ' }

// đánh giá câu trả lời: đúng gửi ngay; sai phải ghi lý do (core dùng làm bài học cho câu hỏi tương tự)
export default function FeedbackBar({
  messageId,
  initial,
  onSaved,
}: {
  messageId: number
  initial?: FeedbackLevel | null
  onSaved: (level: FeedbackLevel) => void
}) {
  const [saved, setSaved] = useState<FeedbackLevel | null>(initial ?? null)
  const [picking, setPicking] = useState<FeedbackLevel | null>(null)
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit(level: FeedbackLevel, why: string) {
    setBusy(true)
    setError('')
    try {
      await sendFeedback(messageId, level, why)
      setSaved(level)
      setPicking(null)
      setReason('')
      onSaved(level)
    } catch (e) {
      setError(String((e as Error).message ?? e))
    } finally {
      setBusy(false)
    }
  }

  function pick(level: FeedbackLevel) {
    if (level === 'dung') {
      submit(level, '')
    } else {
      setPicking(level)
    }
  }

  return (
    <div className="feedback">
      <span className="feedback-label">{saved ? `Đồng chí đã đánh giá: ${LABEL[saved]}` : 'Câu trả lời này:'}</span>
      {LEVELS.map(l => (
        <button
          key={l.level}
          className={`feedback-btn ${l.level} ${saved === l.level || picking === l.level ? 'active' : ''}`}
          disabled={busy}
          onClick={() => pick(l.level)}
        >
          {l.label}
        </button>
      ))}
      {picking && (
        <form
          className="feedback-reason"
          onSubmit={e => {
            e.preventDefault()
            if (reason.trim()) {
              submit(picking, reason.trim())
            }
          }}
        >
          <textarea
            rows={2}
            value={reason}
            placeholder="Sai ở đâu? Ví dụ: số liệu đúng phải là…, thiếu tài liệu…"
            onChange={e => setReason(e.target.value)}
          />
          <div className="feedback-actions">
            <button type="button" className="link" onClick={() => setPicking(null)}>
              Huỷ
            </button>
            <button type="submit" className="send small" disabled={busy || !reason.trim()}>
              Gửi đánh giá
            </button>
          </div>
        </form>
      )}
      {error && <span className="error">{error}</span>}
    </div>
  )
}
