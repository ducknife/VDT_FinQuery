import type { Step } from '../api/types'

// dòng thời gian "hệ thống đang làm gì": bước cuối có chấm đỏ khi còn chạy
export default function Timeline({ steps, running }: { steps: Step[]; running: boolean }) {
  if (steps.length === 0) {
    return null
  }
  return (
    <ul className="timeline">
      {steps.map((s, i) => (
        <li key={i} className={running && i === steps.length - 1 ? 'active' : ''}>
          <span className="dot" />
          <span className="kind">{s.kind}</span>
          <span className={s.kind === 'Kế hoạch' ? 'plan' : ''}>
            {s.text}
            {s.sources && (
              <span className="sources">
                {s.sources.map(src => (
                  <span key={src.id} className="source">
                    [{src.id}] {src.title} · tr. {src.page}
                  </span>
                ))}
              </span>
            )}
          </span>
        </li>
      ))}
    </ul>
  )
}
