// chữ cái đầu của tên (từ cuối trong họ tên kiểu Việt Nam), dùng cho ảnh đại diện
export function initial(displayName: string) {
  const words = displayName.trim().split(/\s+/)
  return (words[words.length - 1]?.[0] ?? '?').toUpperCase()
}
