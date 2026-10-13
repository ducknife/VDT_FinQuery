import { useState } from 'react'
import type { Citation, FeedbackLevel, Turn } from '../api/types'
import AnswerText from './AnswerText'
import FeedbackBar from './FeedbackBar'
import Timeline from './Timeline'

const STATUS: Record<string, { label: string; cls: string }> = {
  answered: { label: 'Đã trả lời', cls: 'ok' },
  partial: { label: 'Trả lời một phần', cls: 'partial' },
  not_found: { label: 'Không tìm thấy', cls: 'partial' },
  out_of_scope: { label: 'Ngoài phạm vi', cls: 'bad' },
}

// một lượt hỏi đáp: câu hỏi, các bước, câu trả lời chạy dần, kết quả kiểm, số đo, đánh giá
export default function TurnView({
  turn,
  onCite,
  onFeedback,
}: {
  turn: Turn
  onCite: (c: Citation) => void
  onFeedback: (level: FeedbackLevel) => void
}) {
  const [stepsOpen, setStepsOpen] = useState(false)
  const showSteps = turn.running || stepsOpen
  const v = turn.verification
  const r = turn.result
  const status = v ? STATUS[v.status] : undefined
  return (
    <div className="turn">
      <div className="question">
        <span>{turn.question}</span>
      </div>
      <div className="card">
        {turn.steps.length > 0 && (
          <>
            {/* đang trả lời: mở để theo dõi; xong thì thu gọn, bấm mới mở rộng */}
            <button
              className="steps-toggle"
              aria-expanded={showSteps}
              onClick={() => setStepsOpen(o => !o)}
              disabled={turn.running}
            >
              <span className="section-title">Các bước ({turn.steps.length})</span>
              {!turn.running && <span className={`chevron ${showSteps ? 'open' : ''}`}>▾</span>}
            </button>
            {showSteps && <Timeline steps={turn.steps} running={turn.running} />}
          </>
        )}
        <p className="section-title">
          {!v && turn.answer && <span className="badge checking">Đang kiểm chứng</span>}
          {status && <span className={`badge ${status.cls}`}>{status.label}</span>}
          {v && v.citations.length > 0 && (
            <span className={`badge ${v.citations.every(c => c.verified) ? 'ok' : 'bad'}`}>
              Trích dẫn {v.citations.filter(c => c.verified).length}/{v.citations.length} đúng nguồn
            </span>
          )}
        </p>
        <AnswerText text={turn.answer} verification={v} onCite={onCite} />
        {v?.missing && <div className="note missing">Chưa tìm thấy: {v.missing}</div>}
        {v && v.unverifiedNumbers.length > 0 && (
          <div className="note warn">Số không có trong nguồn: {v.unverifiedNumbers.join(', ')}</div>
        )}
        {turn.error && <div className="note warn">Lỗi: {turn.error}</div>}
        {r && (
          <div className="meta">
            {(r.timingsMs.total / 1000).toFixed(1)} s · {r.costUsd.total.toFixed(3)} USD · {r.promptTokens.toLocaleString('vi-VN')} token
            vào · {r.completionTokens.toLocaleString('vi-VN')} token ra
          </div>
        )}
        {!turn.running && turn.messageId && (
          <FeedbackBar key={turn.messageId} messageId={turn.messageId} initial={turn.feedback} onSaved={onFeedback} />
        )}
      </div>
    </div>
  )
}
