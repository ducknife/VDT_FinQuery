import { useEffect, useRef, useState } from 'react'
import type { PointerEvent as ReactPointerEvent } from 'react'
import type { ConversationSummary } from '../api/types'

const WIDTH_KEY = 'finq.sidebarWidth'
const MIN_WIDTH = 220
const MAX_WIDTH = 480

function savedWidth() {
  try {
    const w = Number(localStorage.getItem(WIDTH_KEY))
    return w >= MIN_WIDTH && w <= MAX_WIDTH ? w : 280
  } catch {
    return 280
  }
}

// thanh bên: tạo hội thoại mới, mở lại, đổi tên, xoá (chỉ của người đang đăng nhập); kéo mép phải để giãn
export default function ConversationList({
  items,
  activeId,
  disabled,
  onNew,
  onOpen,
  onRename,
  onDelete,
}: {
  items: ConversationSummary[]
  activeId: number | null
  disabled: boolean
  onNew: () => void
  onOpen: (id: number) => void
  onRename: (id: number, title: string) => Promise<void>
  onDelete: (id: number) => Promise<void>
}) {
  const [width, setWidth] = useState(savedWidth)
  const [menu, setMenu] = useState<number | null>(null)
  const [editing, setEditing] = useState<number | null>(null)
  const [draft, setDraft] = useState('')
  const [confirm, setConfirm] = useState<ConversationSummary | null>(null)
  const ref = useRef<HTMLElement>(null)

  // đóng menu khi bấm ra ngoài
  useEffect(() => {
    if (menu === null) {
      return
    }
    const close = () => setMenu(null)
    window.addEventListener('click', close)
    return () => window.removeEventListener('click', close)
  }, [menu])

  function startResize(e: ReactPointerEvent) {
    e.preventDefault()
    const left = ref.current?.getBoundingClientRect().left ?? 0
    const move = (ev: PointerEvent) => setWidth(Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, ev.clientX - left)))
    const up = () => {
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', up)
      document.body.classList.remove('resizing')
      setWidth(w => {
        try {
          localStorage.setItem(WIDTH_KEY, String(w))
        } catch {
          // không lưu được thì chỉ giữ trong phiên
        }
        return w
      })
    }
    document.body.classList.add('resizing')
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', up)
  }

  async function commitRename(c: ConversationSummary) {
    const title = draft.trim()
    setEditing(null)
    if (title && title !== c.title) {
      await onRename(c.id, title)
    }
  }

  return (
    <aside className="sidebar" ref={ref} style={{ width }}>
      <h2 className="sidebar-heading">Trò chuyện</h2>
      <button className="new-chat" disabled={disabled} onClick={onNew}>
        + Cuộc hội thoại mới
      </button>
      <p className="sidebar-title">Lịch sử</p>
      {items.length === 0 && <p className="sidebar-empty">Chưa có cuộc hội thoại nào.</p>}
      <ul className="conversations">
        {items.map(c => (
          <li key={c.id} className={`conversation-row ${c.id === activeId ? 'active' : ''} ${menu === c.id ? 'menu-open' : ''}`}>
            {editing === c.id ? (
              <input
                className="rename"
                autoFocus
                value={draft}
                maxLength={200}
                onChange={e => setDraft(e.target.value)}
                onBlur={() => commitRename(c)}
                onKeyDown={e => {
                  if (e.key === 'Enter') {
                    e.currentTarget.blur()
                  } else if (e.key === 'Escape') {
                    setEditing(null)
                  }
                }}
              />
            ) : (
              <button className="conversation" disabled={disabled} title={c.title} onClick={() => onOpen(c.id)}>
                <span className="conversation-title">{c.title}</span>
                <span className="conversation-time">{formatTime(c.updatedAt)}</span>
              </button>
            )}
            {editing !== c.id && (
              <button
                className="kebab"
                data-tip="Thêm tùy chọn"
                aria-label="Thêm tùy chọn"
                disabled={disabled}
                onClick={e => {
                  e.stopPropagation()
                  setMenu(m => (m === c.id ? null : c.id))
                }}
              >
                ⋮
              </button>
            )}
            {menu === c.id && (
              <div className="menu-pop" onClick={e => e.stopPropagation()}>
                <button
                  onClick={() => {
                    setMenu(null)
                    setDraft(c.title)
                    setEditing(c.id)
                  }}
                >
                  Đổi tên
                </button>
                <button
                  className="danger"
                  onClick={() => {
                    setMenu(null)
                    setConfirm(c)
                  }}
                >
                  Xoá
                </button>
              </div>
            )}
          </li>
        ))}
      </ul>
      <div className="resize-handle" onPointerDown={startResize} title="Kéo để đổi độ rộng" />
      {confirm && (
        <div className="overlay" onClick={() => setConfirm(null)}>
          <div className="dialog" onClick={e => e.stopPropagation()}>
            <h2>Xoá cuộc hội thoại?</h2>
            <p className="page-sub">
              “{confirm.title}” cùng mọi câu hỏi, câu trả lời và đánh giá trong đó sẽ bị xoá vĩnh viễn.
            </p>
            <div className="dialog-actions">
              <button className="outline-btn compact" onClick={() => setConfirm(null)}>
                Huỷ
              </button>
              <button
                className="primary-btn compact"
                onClick={async () => {
                  const id = confirm.id
                  setConfirm(null)
                  await onDelete(id)
                }}
              >
                Xoá
              </button>
            </div>
          </div>
        </div>
      )}
    </aside>
  )
}

function formatTime(iso: string) {
  const d = new Date(iso)
  return Number.isNaN(d.getTime())
    ? ''
    : d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })
}
