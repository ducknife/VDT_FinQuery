import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { createAccount, listAccounts, sendPasswordReset, setAccountEnabled, setAccountRole } from '../api/admin'
import type { Account, AccountRole } from '../api/admin'

const ROLES: { value: AccountRole; label: string }[] = [
  { value: 'pending', label: 'Chờ duyệt' },
  { value: 'analyst', label: 'Nhân viên' },
  { value: 'document_manager', label: 'Quản lý tài liệu' },
  { value: 'admin', label: 'Quản trị' },
]

// quản trị tài khoản (chỉ admin): duyệt, đổi vai trò, khoá/mở, gửi mail đặt lại mật khẩu, tạo tài khoản
export default function UsersPage({ selfId }: { selfId: string }) {
  const [items, setItems] = useState<Account[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState<{ kind: 'ok' | 'warn'; text: string } | null>(null)
  const [creating, setCreating] = useState(false)

  const load = useCallback(async (q: string) => {
    setLoading(true)
    try {
      setItems(await listAccounts(q))
    } catch (e) {
      setMessage({ kind: 'warn', text: String((e as Error).message ?? e) })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const t = setTimeout(() => load(search), 250)
    return () => clearTimeout(t)
  }, [search, load])

  async function act(run: () => Promise<unknown>, ok: string) {
    setMessage(null)
    try {
      await run()
      setMessage({ kind: 'ok', text: ok })
      await load(search)
    } catch (e) {
      setMessage({ kind: 'warn', text: String((e as Error).message ?? e) })
    }
  }

  const pending = items.filter(u => u.role === 'pending').length

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Người dùng</h1>
          <p className="page-sub">
            {items.length} tài khoản{pending > 0 ? ` · ${pending} đang chờ duyệt` : ''}
          </p>
        </div>
        <button className="primary-btn compact" onClick={() => setCreating(true)}>
          + Tạo tài khoản
        </button>
      </div>
      <input
        className="search"
        placeholder="Tìm theo email hoặc tên…"
        value={search}
        onChange={e => setSearch(e.target.value)}
      />
      {message && <div className={`note ${message.kind}`}>{message.text}</div>}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Họ tên</th>
              <th>Email</th>
              <th>Vai trò</th>
              <th>Trạng thái</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {loading && items.length === 0 && (
              <tr>
                <td colSpan={5} className="muted">
                  Đang tải…
                </td>
              </tr>
            )}
            {items.map(u => {
              const self = u.id === selfId
              return (
                <tr key={u.id} className={u.role === 'pending' ? 'row-pending' : ''}>
                  <td>
                    {`${u.lastName ?? ''} ${u.firstName ?? ''}`.trim() || '—'}
                    {self && <span className="tag-self">bạn</span>}
                  </td>
                  <td>{u.email}</td>
                  <td>
                    <select
                      value={u.role}
                      disabled={self}
                      title={self ? 'Không tự đổi vai trò của chính mình' : ''}
                      onChange={e =>
                        act(() => setAccountRole(u.id, e.target.value as AccountRole), `Đã đổi vai trò của ${u.email}`)
                      }
                    >
                      {ROLES.map(r => (
                        <option key={r.value} value={r.value}>
                          {r.label}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <span className={`badge ${u.enabled ? 'ok' : 'bad'}`}>{u.enabled ? 'Hoạt động' : 'Đã khoá'}</span>
                  </td>
                  <td className="actions">
                    <button
                      className="link"
                      onClick={() => act(() => sendPasswordReset(u.id), `Đã gửi mail đặt lại mật khẩu tới ${u.email}`)}
                    >
                      Gửi mail đặt lại mật khẩu
                    </button>
                    {!self && (
                      <button
                        className="link"
                        onClick={() =>
                          act(
                            () => setAccountEnabled(u.id, !u.enabled),
                            `${u.enabled ? 'Đã khoá' : 'Đã mở khoá'} ${u.email}`,
                          )
                        }
                      >
                        {u.enabled ? 'Khoá' : 'Mở khoá'}
                      </button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      {creating && (
        <CreateAccount
          onClose={() => setCreating(false)}
          onCreated={email => {
            setCreating(false)
            setMessage({ kind: 'ok', text: `Đã tạo ${email} và gửi mail để người dùng tự đặt mật khẩu` })
            load(search)
          }}
        />
      )}
    </div>
  )
}

function CreateAccount({ onClose, onCreated }: { onClose: () => void; onCreated: (email: string) => void }) {
  const [email, setEmail] = useState('')
  const [lastName, setLastName] = useState('')
  const [firstName, setFirstName] = useState('')
  const [role, setRole] = useState<AccountRole>('analyst')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await createAccount({ email, firstName, lastName, role })
      onCreated(email)
    } catch (err) {
      setError(String((err as Error).message ?? err))
      setBusy(false)
    }
  }

  return (
    <div className="overlay" onClick={onClose}>
      <form className="dialog" onClick={e => e.stopPropagation()} onSubmit={submit}>
        <h2>Tạo tài khoản</h2>
        <p className="page-sub">Người dùng nhận mail để tự đặt mật khẩu; quản trị viên không biết mật khẩu.</p>
        <label className="field">
          <span>Email công ty</span>
          <input type="email" required value={email} placeholder="ten.ban@viettel.com.vn" onChange={e => setEmail(e.target.value)} />
        </label>
        <div className="field-row">
          <label className="field">
            <span>Họ</span>
            <input required value={lastName} onChange={e => setLastName(e.target.value)} />
          </label>
          <label className="field">
            <span>Tên</span>
            <input required value={firstName} onChange={e => setFirstName(e.target.value)} />
          </label>
        </div>
        <label className="field">
          <span>Vai trò</span>
          <select value={role} onChange={e => setRole(e.target.value as AccountRole)}>
            {ROLES.filter(r => r.value !== 'pending').map(r => (
              <option key={r.value} value={r.value}>
                {r.label}
              </option>
            ))}
          </select>
        </label>
        {error && <div className="note warn">{error}</div>}
        <div className="dialog-actions">
          <button type="button" className="outline-btn compact" onClick={onClose}>
            Huỷ
          </button>
          <button type="submit" className="primary-btn compact" disabled={busy}>
            {busy ? 'Đang tạo…' : 'Tạo và gửi mail'}
          </button>
        </div>
      </form>
    </div>
  )
}
