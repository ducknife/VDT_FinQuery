import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './assets/styles/index.css'
import AuthGate from './components/AuthGate.tsx'
import { applyTheme, currentTheme } from './theme'

// áp chế độ sáng/tối trước khi vẽ để không nháy
applyTheme(currentTheme())

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthGate />
  </StrictMode>,
)
