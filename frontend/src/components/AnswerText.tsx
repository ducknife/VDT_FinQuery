import type { ReactNode } from 'react'
import type { Citation, Verification } from '../api/types'

type Props = {
  text: string
  verification?: Verification
  onCite: (c: Citation) => void
}

// câu trả lời: dòng "- " thành gạch đầu dòng, mã [C1]/[D1] thành nhãn bấm được,
// nhãn đổi màu theo kết quả kiểm (xanh: câu trích có thật trong nguồn, đỏ: không thấy)
export default function AnswerText({ text, verification, onCite }: Props) {
  const lines = text.split('\n')
  const blocks: ReactNode[] = []
  let items: ReactNode[] = []
  const flush = () => {
    if (items.length > 0) {
      blocks.push(<ul key={`ul${blocks.length}`}>{items}</ul>)
      items = []
    }
  }
  lines.forEach((line, i) => {
    const t = line.trim()
    if (t.startsWith('- ') || t.startsWith('• ')) {
      items.push(<li key={i}>{inline(t.slice(2), verification, onCite)}</li>)
      return
    }
    flush()
    if (t) {
      blocks.push(<p key={i}>{inline(t, verification, onCite)}</p>)
    }
  })
  flush()
  return <div className="answer">{blocks}</div>
}

function inline(text: string, verification: Verification | undefined, onCite: (c: Citation) => void) {
  return text.split(/(\[[CD]\d+\])/g).map((part, i) => {
    const m = part.match(/^\[([CD]\d+)\]$/)
    if (!m) {
      return part.replace(/\*\*/g, '')
    }
    const id = m[1]
    const c = verification?.citations.find(x => x.id === id)
    const cls = !verification ? 'cite' : !c ? 'cite' : c.verified ? 'cite ok' : 'cite bad'
    return (
      <button key={i} className={cls} disabled={!c} onClick={() => c && onCite(c)} title={c?.quote ?? 'đang kiểm chứng'}>
        {id}
      </button>
    )
  })
}
