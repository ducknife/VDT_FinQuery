// chế độ sáng/tối: lưu lựa chọn ở localStorage; chưa chọn thì theo hệ điều hành; đặt data-theme trên <html>
export type Theme = 'light' | 'dark'

const KEY = 'finq.theme'

export function currentTheme(): Theme {
  try {
    const saved = localStorage.getItem(KEY)
    if (saved === 'light' || saved === 'dark') {
      return saved
    }
  } catch {
    // trình duyệt chặn localStorage
  }
  return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

// áp lên trang; save = true khi người dùng tự chọn (chưa chọn thì luôn theo hệ điều hành)
export function applyTheme(theme: Theme, save = false) {
  document.documentElement.dataset.theme = theme
  if (!save) {
    return
  }
  try {
    localStorage.setItem(KEY, theme)
  } catch {
    // không lưu được thì chỉ áp cho lần này
  }
}
