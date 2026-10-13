import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { ask } from './api/ask'
import { deleteConversation, getConversation, listConversations, me as fetchMe, renameConversation } from './api/conversations'
import { applyEvent, turnFromMessage } from './api/turn'
import type { Citation, ConversationSummary, Me, Turn } from './api/types'
import { refreshNow } from './auth/auth'
import CitationViewer from './components/CitationViewer'
import ConversationList from './components/ConversationList'
import DocumentsPage from './components/DocumentsPage'
import NavRail from './components/NavRail'
import type { View } from './components/NavRail'
import ProfileDialog from './components/ProfileDialog'
import TurnView from './components/TurnView'
import UsersPage from './components/UsersPage'
import { applyTheme, currentTheme } from './theme'
import type { Theme } from './theme'

// câu hỏi gợi ý ở màn hình hội thoại mới: lấy từ tập kiểm thử (data/eval), đều đã trả lời đúng khi chấm
const SAMPLES = [
  // C01: chỉ nhánh số liệu (CSDL)
  'Doanh thu thuần hợp nhất năm 2025 tăng bao nhiêu phần trăm so với năm 2024?',
  // V04: chỉ nhánh tài liệu
  'Cổ tức năm 2024 được chi trả theo tỷ lệ bao nhiêu và ngày đăng ký cuối cùng là ngày nào?',
  // M01: cả hai nhánh (kế hoạch trong tài liệu, số thực hiện trong CSDL)
  'Năm 2025 công ty có hoàn thành kế hoạch lợi nhuận sau thuế hợp nhất không?',
  // M03: cả hai nhánh và phép tính chênh lệch
  'So sánh LNST hợp nhất quý 2/2024 với quý 3/2025.',
]

// lời chào theo giờ trong ngày
function partOfDay() {
  const h = new Date().getHours()
  return h < 11 ? 'buổi sáng' : h < 14 ? 'buổi trưa' : h < 18 ? 'buổi chiều' : 'buổi tối'
}

// mở thẳng một màn hình bằng địa chỉ: #tai-lieu, #nguoi-dung
const HASH_VIEW: Record<string, View> = { '#hoi-dap': 'chat', '#tai-lieu': 'documents', '#nguoi-dung': 'users' }

function App({ me: initialMe }: { me: Me }) {
  // thông tin người dùng: cập nhật lại sau khi tự sửa họ tên
  const [me, setMe] = useState(initialMe)
  const [theme, setTheme] = useState<Theme>(currentTheme)
  const [profileOpen, setProfileOpen] = useState(false)
  // màn hình theo vai trò: ai cũng hỏi đáp; quản lý tài liệu thêm Tài liệu; quản trị thêm Người dùng
  const [view, setViewState] = useState<View>(() => HASH_VIEW[location.hash] ?? 'chat')
  function setView(v: View) {
    setViewState(v)
    history.replaceState(null, '', Object.keys(HASH_VIEW).find(h => HASH_VIEW[h] === v) ?? '#hoi-dap')
  }
  const allTabs: { id: View; label: string; show: boolean }[] = [
    { id: 'chat', label: 'Hỏi đáp', show: me.permissions.ask },
    { id: 'documents', label: 'Tài liệu', show: me.permissions.manageDocuments },
    { id: 'users', label: 'Người dùng', show: me.permissions.manageUsers },
  ]
  const tabs = allTabs.filter(t => t.show)
  const [turns, setTurns] = useState<Turn[]>([])
  const [input, setInput] = useState('')
  const [viewing, setViewing] = useState<Citation | null>(null)
  const [conversations, setConversations] = useState<ConversationSummary[]>([])
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [loadError, setLoadError] = useState('')
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const bottom = useRef<HTMLDivElement>(null)
  const running = turns.some(t => t.running)

  const refreshList = useCallback(() => {
    listConversations()
      .then(setConversations)
      .catch(e => setLoadError(`Không tải được lịch sử: ${String((e as Error).message ?? e)}`))
  }, [])

  useEffect(() => {
    refreshList()
  }, [refreshList])

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth' })
  }, [turns])

  async function submit(question: string) {
    const q = question.trim()
    if (!q || running) {
      return
    }
    const id = Date.now()
    setInput('')
    setTurns(ts => [...ts, { id, question: q, steps: [], answer: '', running: true }])
    const update = (fn: (t: Turn) => Turn) => setTurns(ts => ts.map(t => (t.id === id ? fn(t) : t)))
    try {
      await ask(q, conversationId, e => {
        // hội thoại mới: core tạo và trả mã ở sự kiện "saved"; các câu sau gửi kèm mã này
        if (e.type === 'saved') {
          setConversationId(Number(e.data.conversationId))
        }
        update(t => applyEvent(t, e))
      })
      update(t => ({ ...t, running: false }))
    } catch (err) {
      update(t => ({ ...t, error: String((err as Error).message ?? err), running: false }))
    } finally {
      refreshList()
    }
  }

  function newConversation() {
    setTurns([])
    setConversationId(null)
    setSidebarOpen(false)
  }

  async function openConversation(id: number) {
    setLoadError('')
    try {
      const c = await getConversation(id)
      setTurns(c.messages.map(turnFromMessage))
      setConversationId(c.id)
      setSidebarOpen(false)
    } catch (e) {
      setLoadError(`Không mở được hội thoại: ${String((e as Error).message ?? e)}`)
    }
  }

  async function renameOne(id: number, title: string) {
    try {
      await renameConversation(id, title)
      refreshList()
    } catch (e) {
      setLoadError(`Không đổi được tên: ${String((e as Error).message ?? e)}`)
    }
  }

  async function deleteOne(id: number) {
    try {
      await deleteConversation(id)
      if (id === conversationId) {
        newConversation()
      }
      refreshList()
    } catch (e) {
      setLoadError(`Không xoá được: ${String((e as Error).message ?? e)}`)
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    submit(input)
  }

  return (
    <div className="app">
      <NavRail
        tabs={tabs}
        view={view}
        onView={setView}
        theme={theme}
        onTheme={t => {
          setTheme(t)
          applyTheme(t, true)
        }}
        displayName={me.displayName || me.name}
        onProfile={() => setProfileOpen(true)}
      />
      <div className="workspace">
      {view === 'documents' && <DocumentsPage />}
      {view === 'users' && <UsersPage selfId={me.id} />}
      <div className={`body ${sidebarOpen ? 'sidebar-open' : ''}`} hidden={view !== 'chat'}>
        <ConversationList
          items={conversations}
          activeId={conversationId}
          disabled={running}
          onNew={newConversation}
          onOpen={openConversation}
          onRename={renameOne}
          onDelete={deleteOne}
        />
        <div className="chat">
          <main className="main">
            <button className="history-toggle" onClick={() => setSidebarOpen(o => !o)}>
              ☰ Lịch sử
            </button>
            {loadError && <div className="note warn">{loadError}</div>}
            {turns.length === 0 && (
              <div className="empty">
                <h1>Chào {partOfDay()}, đồng chí {me.displayName || me.name}!</h1>
                <p className="empty-lead">Hôm nay đồng chí cần tra cứu thông tin gì?</p>
                <p>
                  Tôi trả lời từ báo cáo tài chính, báo cáo thường niên, nghị quyết và công bố thông tin của doanh nghiệp. Mỗi
                  câu trả lời đều kèm trích dẫn tới đúng tài liệu và trang; bấm vào mã trích dẫn để xem trang gốc.
                </p>
                <p className="samples-title">Gợi ý cho đồng chí</p>
                <div className="samples">
                  {SAMPLES.map(s => (
                    <button key={s} className="sample" onClick={() => submit(s)}>
                      {s}
                    </button>
                  ))}
                </div>
              </div>
            )}
            {turns.map(t => (
              <TurnView
                key={t.id}
                turn={t}
                onCite={setViewing}
                onFeedback={level => setTurns(ts => ts.map(x => (x.id === t.id ? { ...x, feedback: level } : x)))}
              />
            ))}
            <div ref={bottom} />
          </main>
          <div className="composer">
            <form onSubmit={onSubmit}>
              <textarea
                rows={1}
                value={input}
                placeholder={
                  conversationId
                    ? 'Hỏi tiếp, ví dụ: còn năm trước thì sao?'
                    : 'Nhập câu hỏi, ví dụ: ROE hợp nhất năm 2025 là bao nhiêu?'
                }
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault()
                    submit(input)
                  }
                }}
              />
              <button className="send" type="submit" disabled={running || !input.trim()}>
                {running ? 'Đang trả lời…' : 'Gửi'}
              </button>
            </form>
          </div>
        </div>
      </div>
      </div>
      {viewing && <CitationViewer citation={viewing} onClose={() => setViewing(null)} />}
      {profileOpen && (
        <ProfileDialog
          me={me}
          onClose={() => setProfileOpen(false)}
          onUpdated={async () => {
            // token cũ còn họ tên cũ: xin token mới rồi hỏi lại core
            await refreshNow()
            setMe(await fetchMe())
          }}
        />
      )}
    </div>
  )
}

export default App
