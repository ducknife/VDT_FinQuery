import { useState } from 'react'
import type { FormEvent } from 'react'
import { changePassword, updateProfile } from '../api/conversations'
import type { Me } from '../api/types'
import { authEnabled, logout } from '../auth/auth'
import { initial } from '../utils/name'

const ROLE_LABEL: Record<string, string> = { admin: 'Quản trị', document_manager: 'Quản lý tài liệu', analyst: 'Nhân viên' }

// thông tin cá nhân: sửa họ tên, đổi mật khẩu, đăng xuất
export default function ProfileDialog({
  me,
  onClose,
  onUpdated,
}: {
  me: Me
  onClose: () => void
  onUpdated: () => Promise<void>
}) {
  // họ tên kiểu Việt Nam: từ cuối là tên, phần trước là họ
  const words = (me.displayName || '').trim().split(/\s+/)
  const [lastName, setLastName] = useState(words.length > 1 ? words.slice(0, -1).join(' ') : '')
  const [firstName, setFirstName] = useState(words.length > 0 ? words[words.length - 1] : '')
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [again, setAgain] = useState('')
  const [busy, setBusy] = useState<'' | 'name' | 'password'>('')
  const [message, setMessage] = useState<{ kind: 'ok' | 'warn'; text: string } | null>(null)
  const role = ['admin', 'document_manager', 'analyst'].find(r => me.roles.includes(r))

  async function saveName(e: FormEvent) {
    e.preventDefault()
    setBusy('name')
    setMessage(null)
    try {
      await updateProfile(lastName.trim(), firstName.trim())
      await onUpdated()
      setMessage({ kind: 'ok', text: 'Đã cập nhật họ tên.' })
    } catch (err) {
      setMessage({ kind: 'warn', text: String((err as Error).message ?? err) })
    } finally {
      setBusy('')
    }
  }

  async function savePassword(e: FormEvent) {
    e.preventDefault()
    if (next !== again) {
      setMessage({ kind: 'warn', text: 'Mật khẩu nhập lại không khớp.' })
      return
    }
    setBusy('password')
    setMessage(null)
    try {
      await changePassword(current, next)
      setCurrent('')
      setNext('')
      setAgain('')
      setMessage({ kind: 'ok', text: 'Đã đổi mật khẩu. Lần đăng nhập sau dùng mật khẩu mới.' })
    } catch (err) {
      setMessage({ kind: 'warn', text: String((err as Error).message ?? err) })
    } finally {
      setBusy('')
    }
  }

  return (
    <div className="overlay" onClick={onClose}>
      <div className="dialog profile" onClick={e => e.stopPropagation()}>
        <div className="profile-head">
          <span className="avatar big">{initial(me.displayName || me.name)}</span>
          <div>
            <h2>{me.displayName || me.name}</h2>
            <p className="page-sub">
              {me.name}
              {role && <span className={`role role-${role}`}>{ROLE_LABEL[role]}</span>}
            </p>
          </div>
          <button className="close" onClick={onClose} aria-label="Đóng">
            ✕
          </button>
        </div>
        {message && <div className={`note ${message.kind}`}>{message.text}</div>}
        {authEnabled ? (
          <>
            <form className="profile-section" onSubmit={saveName}>
              <h3>Họ tên</h3>
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
              <div className="dialog-actions">
                <button type="submit" className="primary-btn compact" disabled={busy !== ''}>
                  {busy === 'name' ? 'Đang lưu…' : 'Lưu họ tên'}
                </button>
              </div>
            </form>
            <form className="profile-section" onSubmit={savePassword}>
              <h3>Đổi mật khẩu</h3>
              <label className="field">
                <span>Mật khẩu hiện tại</span>
                <input type="password" required autoComplete="current-password" value={current} onChange={e => setCurrent(e.target.value)} />
              </label>
              <div className="field-row">
                <label className="field">
                  <span>Mật khẩu mới</span>
                  <input type="password" required autoComplete="new-password" value={next} onChange={e => setNext(e.target.value)} />
                </label>
                <label className="field">
                  <span>Nhập lại</span>
                  <input type="password" required autoComplete="new-password" value={again} onChange={e => setAgain(e.target.value)} />
                </label>
              </div>
              <p className="gate-note">
                Từ 12 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt; không chứa dấu nháy, tên công ty, tên đăng nhập.
              </p>
              <div className="dialog-actions">
                <button type="submit" className="primary-btn compact" disabled={busy !== ''}>
                  {busy === 'password' ? 'Đang đổi…' : 'Đổi mật khẩu'}
                </button>
              </div>
            </form>
            <div className="dialog-actions">
              <button className="outline-btn compact" onClick={logout}>
                Đăng xuất
              </button>
            </div>
          </>
        ) : (
          <p className="page-sub">Đang chạy không đăng nhập (chế độ dev): không có tài khoản để sửa.</p>
        )}
      </div>
    </div>
  )
}
