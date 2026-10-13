import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { me as fetchMe } from '../api/conversations'
import type { Me } from '../api/types'
import App from '../App'
import { forgotPassword } from '../api/admin'
import visual from '../assets/images/login-hero.svg'
import { authEnabled, isLoggedIn, login, logout, onSessionExpired } from '../auth/auth'
import Logo from './Logo'

type State =
  | { kind: 'loading' }
  | { kind: 'login'; notice?: string }
  | { kind: 'forgot' }
  | { kind: 'pending'; me: Me }
  | { kind: 'ready'; me: Me }
  | { kind: 'error'; message: string }

// hỏi core /api/v1/me: có quyền thì vào, chưa có vai trò thì chờ duyệt; lỗi vì hết phiên thì về đăng nhập
async function resolve(): Promise<State> {
  try {
    const me = await fetchMe()
    return me.permissions.ask ? { kind: 'ready', me } : { kind: 'pending', me }
  } catch (e) {
    return isLoggedIn() ? { kind: 'error', message: String((e as Error)?.message ?? e) } : { kind: 'login' }
  }
}

// trước khi vào ứng dụng: chưa đăng nhập thì hiện trang đăng nhập của FinQuery -> hỏi core /api/v1/me lấy vai trò
// -> chưa được duyệt thì báo chờ duyệt
export default function AuthGate() {
  const [state, setState] = useState<State>(() => (isLoggedIn() ? { kind: 'loading' } : { kind: 'login' }))

  const enter = useCallback(() => {
    setState({ kind: 'loading' })
    resolve().then(setState)
  }, [])

  // mở trang khi đã có phiên từ trước: hỏi core vai trò
  useEffect(() => {
    if (!isLoggedIn()) {
      return
    }
    let cancelled = false
    resolve().then(s => {
      if (!cancelled) {
        setState(s)
      }
    })
    return () => {
      cancelled = true
    }
  }, [])

  // phiên hết hạn hoặc đăng xuất: về trang đăng nhập
  useEffect(
    () =>
      onSessionExpired(() =>
        setState({ kind: 'login', notice: 'Phiên đăng nhập đã kết thúc. Vui lòng đăng nhập lại.' }),
      ),
    [],
  )

  if (state.kind === 'ready') {
    return <App me={state.me} />
  }
  // bố cục trang đăng nhập: ảnh thương hiệu bên trái, nội dung bên phải
  return (
    <div className="gate">
      <div className="gate-visual">
        <img src={visual} alt="" />
        <div className="gate-hero-text">
          <h2>Truy xuất dữ liệu,</h2>
          <h2>hỏi đáp tài chính</h2>
          <p>Mỗi con số dẫn tới đúng tài liệu và trang gốc</p>
        </div>
      </div>
      <div className="gate-panel">
        <div className="gate-content">
          <Logo size={140} />
          {state.kind === 'loading' && <p className="gate-text">Đang kiểm tra phiên đăng nhập…</p>}
          {state.kind === 'login' && (
            <LoginForm notice={state.notice} onDone={enter} onForgot={() => setState({ kind: 'forgot' })} />
          )}
          {state.kind === 'forgot' && <ForgotForm onBack={() => setState({ kind: 'login' })} />}
          {state.kind === 'pending' && (
            <>
              <h1 className="gate-title">Chờ duyệt</h1>
              <p className="gate-text">
                Chào đồng chí <strong>{state.me.displayName || state.me.name}</strong>. Tài khoản của đồng chí đang chờ quản trị viên duyệt,
                nên chưa dùng được FinQuery.
              </p>
              <button className="outline-btn" onClick={logout}>
                Đăng xuất
              </button>
            </>
          )}
          {state.kind === 'error' && (
            <>
              <h1 className="gate-title">Không vào được</h1>
              <p className="gate-text error">{state.message}</p>
              <p className="gate-note">Kiểm tra backend (localhost:8080) đã chạy chưa.</p>
              <button className="primary-btn" onClick={() => location.reload()}>
                Thử lại
              </button>
            </>
          )}
        </div>
        <p className="gate-footer">© 2026 FinQuery</p>
      </div>
    </div>
  )
}

function LoginForm({ notice, onDone, onForgot }: { notice?: string; onDone: () => void; onForgot: () => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [show, setShow] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!email.trim() || !password) {
      return
    }
    setBusy(true)
    setError('')
    try {
      await login(email, password)
      setPassword('')
      onDone()
    } catch (err) {
      setError(String((err as Error)?.message ?? err))
      setBusy(false)
    }
  }

  if (!authEnabled) {
    return (
      <button className="primary-btn" onClick={onDone}>
        Vào FinQuery
      </button>
    )
  }
  return (
    <form className="login" onSubmit={submit}>
      <h1 className="gate-title">Đăng nhập</h1>
      <p className="gate-text">Vui lòng nhập email công ty và mật khẩu</p>
      {notice && <div className="note warn">{notice}</div>}
      <label className="field">
        <span>Email công ty</span>
        <input
          type="email"
          autoComplete="username"
          placeholder="ten.ban@viettel.com.vn"
          value={email}
          onChange={e => setEmail(e.target.value)}
          autoFocus
        />
      </label>
      <label className="field">
        <span>Mật khẩu</span>
        <span className="password">
          <input
            type={show ? 'text' : 'password'}
            autoComplete="current-password"
            placeholder="Nhập mật khẩu"
            value={password}
            onChange={e => setPassword(e.target.value)}
          />
          <button type="button" className="link" onClick={() => setShow(s => !s)}>
            {show ? 'Ẩn' : 'Hiện'}
          </button>
        </span>
      </label>
      {error && <div className="note warn">{error}</div>}
      <button className="primary-btn" type="submit" disabled={busy || !email.trim() || !password}>
        <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="1.8">
          <rect x="3" y="7" width="18" height="11" rx="3" />
          <path d="M7.5 12.5h.01M11 12.5h.01M14.5 12.5h.01" strokeWidth="2.6" strokeLinecap="round" />
        </svg>
        {busy ? 'Đang đăng nhập…' : 'Đăng nhập bằng mật khẩu'}
      </button>
      <div className="login-foot">
        <button type="button" className="link" onClick={onForgot}>
          Quên mật khẩu?
        </button>
        <span className="gate-note">Tài khoản do quản trị viên FinQuery cấp</span>
      </div>
    </form>
  )
}

// quên mật khẩu ngay trên FinQuery: core nhờ Keycloak gửi mail có link đặt mật khẩu mới
function ForgotForm({ onBack }: { onBack: () => void }) {
  const [email, setEmail] = useState('')
  const [busy, setBusy] = useState(false)
  const [sent, setSent] = useState('')
  const [error, setError] = useState('')

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      setSent((await forgotPassword(email.trim())).message)
    } catch (err) {
      setError(String((err as Error)?.message ?? err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="login" onSubmit={submit}>
      <h1 className="gate-title">Quên mật khẩu</h1>
      <p className="gate-text">Nhập email công ty, FinQuery sẽ gửi đường dẫn để bạn đặt mật khẩu mới</p>
      {sent ? (
        <div className="note ok">{sent}</div>
      ) : (
        <>
          <label className="field">
            <span>Email công ty</span>
            <input
              type="email"
              required
              autoComplete="username"
              placeholder="ten.ban@viettel.com.vn"
              value={email}
              onChange={e => setEmail(e.target.value)}
              autoFocus
            />
          </label>
          {error && <div className="note warn">{error}</div>}
          <button className="primary-btn" type="submit" disabled={busy || !email.trim()}>
            {busy ? 'Đang gửi…' : 'Gửi đường dẫn đặt lại mật khẩu'}
          </button>
        </>
      )}
      <div className="login-foot center">
        <button type="button" className="link" onClick={onBack}>
          ‹ Quay lại đăng nhập
        </button>
      </div>
    </form>
  )
}
