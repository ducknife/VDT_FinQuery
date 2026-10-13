import { useEffect, useRef, useState } from 'react'
import { pageImage, pageText } from '../api/ask'
import type { Citation } from '../api/types'

// bấm trích dẫn: ảnh trang PDF gốc (trái) và chữ OCR của trang (phải), câu trích được bôi nền
export default function CitationViewer({ citation, onClose }: { citation: Citation; onClose: () => void }) {
  const [text, setText] = useState<string>('')
  const [image, setImage] = useState<string>('')
  const [error, setError] = useState<string>('')
  // phóng ảnh trang: 100% = rộng bằng khung
  const [zoom, setZoom] = useState(100)
  const zoomBy = (d: number) => setZoom(z => Math.min(300, Math.max(50, z + d)))
  const pageBox = useRef<HTMLDivElement>(null)

  // Ctrl + lăn chuột để phóng ảnh (sự kiện gắn passive: false thì mới chặn được trình duyệt phóng cả trang)
  useEffect(() => {
    const box = pageBox.current
    if (!box) {
      return
    }
    const onWheel = (e: WheelEvent) => {
      if (e.ctrlKey) {
        e.preventDefault()
        setZoom(z => Math.min(300, Math.max(50, z + (e.deltaY < 0 ? 10 : -10))))
      }
    }
    box.addEventListener('wheel', onWheel, { passive: false })
    return () => box.removeEventListener('wheel', onWheel)
  }, [image])
  const hasPage = !!citation.taiLieuId && citation.trang > 0

  useEffect(() => {
    if (!hasPage) {
      return
    }
    let url = ''
    let cancelled = false
    pageText(citation.taiLieuId!, citation.trang).then(setText).catch(e => setError(String(e.message ?? e)))
    pageImage(citation.taiLieuId!, citation.trang)
      .then(u => {
        url = u
        if (cancelled) {
          URL.revokeObjectURL(u)
        } else {
          setImage(u)
        }
      })
      .catch(e => setError(String(e.message ?? e)))
    // URL blob giữ ảnh trong bộ nhớ: thu hồi khi đóng
    return () => {
      cancelled = true
      if (url) {
        URL.revokeObjectURL(url)
      }
    }
  }, [citation, hasPage])

  return (
    <div className="overlay" onClick={onClose}>
      <div className="viewer" onClick={e => e.stopPropagation()}>
        <div className="viewer-head">
          <span className={`badge ${citation.verified ? 'ok' : 'bad'}`}>
            [{citation.id}] {citation.verified ? 'đúng nguồn' : 'không thấy trong nguồn'}
          </span>
          <strong>
            {hasPage ? `${citation.tenHienThi ?? citation.taiLieuId} · trang ${citation.trang} của file` : 'Số liệu do hệ thống tính'}
          </strong>
          <button className="close" onClick={onClose} aria-label="Đóng">
            ✕
          </button>
        </div>
        <div className="viewer-quote">“{citation.quote}”</div>
        {hasPage ? (
          <div className="viewer-body">
            <div className="page-pane">
              <div className="zoom-bar">
                <button onClick={() => zoomBy(-25)} aria-label="Thu nhỏ" disabled={zoom <= 50}>
                  −
                </button>
                <button className="zoom-value" onClick={() => setZoom(100)} title="Về vừa khung">
                  {zoom}%
                </button>
                <button onClick={() => zoomBy(25)} aria-label="Phóng to" disabled={zoom >= 300}>
                  +
                </button>
              </div>
              <div className="page-image" ref={pageBox} title="Ctrl + lăn chuột để phóng to, thu nhỏ">
                {image ? (
                  <img src={image} alt={`Trang ${citation.trang}`} style={{ width: `${zoom}%` }} />
                ) : (
                  <div className="image-loading">Đang tải ảnh trang…</div>
                )}
              </div>
            </div>
            <pre>{error ? <span className="error">{error}</span> : highlight(text, citation.quote)}</pre>
          </div>
        ) : (
          <div className="viewer-body">
            <p>Bằng chứng này là kết quả hệ thống tính từ các số liệu đã dẫn, không gắn với một trang tài liệu.</p>
          </div>
        )}
      </div>
    </div>
  )
}

// tìm câu trích trong chữ OCR, bỏ qua khác biệt khoảng trắng và ký hiệu bảng
function highlight(text: string, quote: string) {
  if (!text) {
    return 'Đang tải…'
  }
  const words = quote.split(/[\s|*#]+/).filter(Boolean).slice(0, 25)
  if (words.length === 0) {
    return text
  }
  const pattern = new RegExp(words.map(w => w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('[\\s|*#]+'), 'i')
  const m = text.match(pattern)
  if (!m || m.index === undefined) {
    return text
  }
  return (
    <>
      {text.slice(0, m.index)}
      <mark>{m[0]}</mark>
      {text.slice(m.index + m[0].length)}
    </>
  )
}
