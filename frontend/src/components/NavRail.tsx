import type { ReactNode } from 'react'
import type { Theme } from '../theme'
import { initial } from '../utils/name'
import Logo from './Logo'

export type View = 'chat' | 'documents' | 'users'

const ICONS: Record<View | 'sun' | 'moon', ReactNode> = {
  chat: (
    <path d="M12 4c4.97 0 9 3.36 9 7.5S16.97 19 12 19c-.97 0-1.9-.13-2.78-.36L5 20l1.1-3.3C4.17 15.37 3 13.53 3 11.5 3 7.36 7.03 4 12 4Zm-4 7.5h.01M12 11.5h.01M16 11.5h.01" />
  ),
  documents: (
    <path d="M7 3h8l4 4v13a1 1 0 0 1-1 1H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Zm8 0v4h4M9 12h6M9 16h6M9 8h2" />
  ),
  users: (
    <path d="M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Zm-6 9c0-3.31 2.69-6 6-6s6 2.69 6 6M16 4.5a3.5 3.5 0 0 1 0 6.5M18 14.5c1.8.9 3 2.9 3 5.5" />
  ),
  sun: (
    <path d="M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0-13v2m0 14v2M3 12h2m14 0h2M5.64 5.64l1.41 1.41m9.9 9.9 1.41 1.41M5.64 18.36l1.41-1.41m9.9-9.9 1.41-1.41" />
  ),
  moon: <path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5Z" />,
}

function Icon({ name }: { name: keyof typeof ICONS }) {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {ICONS[name]}
    </svg>
  )
}

// thanh biểu tượng bên trái: màn hình theo vai trò, sáng/tối, thông tin cá nhân
export default function NavRail({
  tabs,
  view,
  onView,
  theme,
  onTheme,
  displayName,
  onProfile,
}: {
  tabs: { id: View; label: string }[]
  view: View
  onView: (v: View) => void
  theme: Theme
  onTheme: (t: Theme) => void
  displayName: string
  onProfile: () => void
}) {
  return (
    <nav className="rail" aria-label="Điều hướng">
      <div className="rail-logo">
        <Logo size={44} />
      </div>
      {tabs.map(t => (
        <button
          key={t.id}
          className={`rail-btn ${view === t.id ? 'active' : ''}`}
          data-tip={t.label}
          aria-label={t.label}
          aria-current={view === t.id ? 'page' : undefined}
          onClick={() => onView(t.id)}
        >
          <Icon name={t.id} />
        </button>
      ))}
      <span className="rail-spacer" />
      <div className="theme-switch" role="radiogroup" aria-label="Chế độ hiển thị">
        {(['light', 'dark'] as Theme[]).map(t => (
          <button
            key={t}
            className={theme === t ? 'on' : ''}
            role="radio"
            aria-checked={theme === t}
            data-tip={t === 'light' ? 'Chế độ sáng' : 'Chế độ tối'}
            onClick={() => onTheme(t)}
          >
            <Icon name={t === 'light' ? 'sun' : 'moon'} />
          </button>
        ))}
      </div>
      <button className="avatar" data-tip="Thông tin cá nhân" aria-label="Thông tin cá nhân" onClick={onProfile}>
        {initial(displayName)}
      </button>
    </nav>
  )
}
