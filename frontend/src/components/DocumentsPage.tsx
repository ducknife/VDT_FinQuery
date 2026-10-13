import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { listDocuments, reindexDocument, uploadDocument } from '../api/admin'
import type { DocumentItem } from '../api/admin'

const LOAI: Record<string, string> = {
  bctc: 'Báo cáo tài chính',
  bctn: 'Báo cáo thường niên',
  giai_trinh: 'Công văn giải trình',
  bao_cao_pt: 'Báo cáo phát triển bền vững',
  to_trinh: 'Tờ trình ĐHĐCĐ',
  nghi_quyet: 'Nghị quyết, biên bản ĐHĐCĐ',
  bao_cao_dhdcd: 'Báo cáo trình ĐHĐCĐ',
  tai_lieu_hop: 'Tài liệu họp ĐHĐCĐ',
  cbtt_co_tuc: 'CBTT: cổ tức',
  cbtt_phat_hanh: 'CBTT: phát hành, tăng vốn',
  cbtt_khac: 'CBTT khác',
  dinh_chinh: 'Đính chính',
  ke_hoach: 'Kế hoạch kinh doanh',
}

const KY: Record<string, string> = { nam: 'Năm', ban_nien: 'Bán niên', Q1: 'Quý 1', Q2: 'Quý 2', Q3: 'Quý 3', Q4: 'Quý 4' }

const STATUS: Record<DocumentItem['trangThai'], { label: string; cls: string }> = {
  cho_ocr: { label: 'Chờ OCR', cls: 'partial' },
  cho_embed: { label: 'Chờ index', cls: 'checking' },
  da_embed: { label: 'Đã index', cls: 'ok' },
}

// quản lý tài liệu (document_manager, admin): xem kho, tải lên, chạy index lại
export default function DocumentsPage() {
  const [data, setData] = useState<{ items: DocumentItem[]; total: number }>({ items: [], total: 0 })
  const [q, setQ] = useState('')
  const [loai, setLoai] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState<{ kind: 'ok' | 'warn'; text: string } | null>(null)
  const [uploading, setUploading] = useState(false)
  const size = 15

  const load = useCallback(async (query: string, type: string, p: number) => {
    setLoading(true)
    try {
      const r = await listDocuments(query, type, p)
      setData({ items: r.items, total: r.total })
    } catch (e) {
      setMessage({ kind: 'warn', text: String((e as Error).message ?? e) })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const t = setTimeout(() => load(q, loai, page), 250)
    return () => clearTimeout(t)
  }, [q, loai, page, load])

  const pages = Math.max(1, Math.ceil(data.total / size))

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Tài liệu</h1>
          <p className="page-sub">{data.total.toLocaleString('vi-VN')} tài liệu trong kho</p>
        </div>
        <button className="primary-btn compact" onClick={() => setUploading(true)}>
          + Tải tài liệu lên
        </button>
      </div>
      <div className="filters">
        <input
          className="search"
          placeholder="Tìm theo tên hoặc mã tài liệu…"
          value={q}
          onChange={e => {
            setQ(e.target.value)
            setPage(0)
          }}
        />
        <select
          value={loai}
          onChange={e => {
            setLoai(e.target.value)
            setPage(0)
          }}
        >
          <option value="">Mọi loại</option>
          {Object.entries(LOAI).map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </select>
        <button className="outline-btn compact" onClick={() => load(q, loai, page)}>
          Làm mới
        </button>
      </div>
      {message && <div className={`note ${message.kind}`}>{message.text}</div>}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Tài liệu</th>
              <th>Loại</th>
              <th>Kỳ</th>
              <th>Ban hành</th>
              <th>Trang</th>
              <th>Trạng thái</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {loading && data.items.length === 0 && (
              <tr>
                <td colSpan={7} className="muted">
                  Đang tải…
                </td>
              </tr>
            )}
            {!loading && data.items.length === 0 && (
              <tr>
                <td colSpan={7} className="muted">
                  Không có tài liệu nào khớp.
                </td>
              </tr>
            )}
            {data.items.map(d => (
              <tr key={d.taiLieuId}>
                <td>
                  <div className="doc-name">{d.tenHienThi}</div>
                  <div className="doc-id">{d.taiLieuId}</div>
                </td>
                <td>{LOAI[d.loai] ?? d.loai}</td>
                <td>
                  {[d.ky ? KY[d.ky] ?? d.ky : null, d.nam, d.phamVi === 'hop_nhat' ? 'hợp nhất' : d.phamVi === 'rieng' ? 'riêng' : null]
                    .filter(Boolean)
                    .join(' · ') || '—'}
                </td>
                <td>{d.ngayBanHanh ? new Date(d.ngayBanHanh).toLocaleDateString('vi-VN') : '—'}</td>
                <td>{d.tongSoTrang || '—'}</td>
                <td>
                  <span className={`badge ${STATUS[d.trangThai].cls}`}>{STATUS[d.trangThai].label}</span>
                  {d.soChunk ? <span className="doc-id"> {d.soChunk} chunk</span> : null}
                </td>
                <td className="actions">
                  <button
                    className="link"
                    onClick={async () => {
                      try {
                        await reindexDocument(d.taiLieuId)
                        setMessage({ kind: 'ok', text: `Đã xếp hàng index lại ${d.tenHienThi}` })
                      } catch (e) {
                        setMessage({ kind: 'warn', text: String((e as Error).message ?? e) })
                      }
                    }}
                  >
                    Index lại
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="pager">
        <button className="outline-btn compact" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
          ‹ Trước
        </button>
        <span>
          Trang {page + 1}/{pages}
        </span>
        <button className="outline-btn compact" disabled={page + 1 >= pages} onClick={() => setPage(p => p + 1)}>
          Sau ›
        </button>
      </div>
      {uploading && (
        <UploadDialog
          onClose={() => setUploading(false)}
          onUploaded={name => {
            setUploading(false)
            setMessage({ kind: 'ok', text: `Đã tải lên ${name}; hệ thống đang OCR và index (chạy nền)` })
            load(q, loai, 0)
          }}
        />
      )}
    </div>
  )
}

function UploadDialog({ onClose, onUploaded }: { onClose: () => void; onUploaded: (name: string) => void }) {
  const [file, setFile] = useState<File | null>(null)
  const [meta, setMeta] = useState({
    tenHienThi: '',
    loai: 'bctc',
    nam: String(new Date().getFullYear()),
    ky: '',
    phamVi: '',
    ngayKetThucKy: '',
    ngayBanHanh: '',
    maCongTy: 'CTR',
  })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const set = (k: keyof typeof meta) => (e: { target: { value: string } }) => setMeta(m => ({ ...m, [k]: e.target.value }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!file) {
      return
    }
    setBusy(true)
    setError('')
    try {
      await uploadDocument(file, meta)
      onUploaded(meta.tenHienThi)
    } catch (err) {
      setError(String((err as Error).message ?? err))
      setBusy(false)
    }
  }

  return (
    <div className="overlay" onClick={onClose}>
      <form className="dialog" onClick={e => e.stopPropagation()} onSubmit={submit}>
        <h2>Tải tài liệu lên</h2>
        <p className="page-sub">File PDF. Sau khi tải lên, hệ thống tự OCR, làm sạch, chia đoạn, index và trích số liệu.</p>
        <label className="field">
          <span>File PDF</span>
          <input type="file" accept="application/pdf" required onChange={e => setFile(e.target.files?.[0] ?? null)} />
        </label>
        <label className="field">
          <span>Tên hiển thị</span>
          <input required value={meta.tenHienThi} placeholder="Báo cáo tài chính hợp nhất quý 3 2026" onChange={set('tenHienThi')} />
        </label>
        <div className="field-row">
          <label className="field">
            <span>Loại</span>
            <select value={meta.loai} onChange={set('loai')}>
              {Object.entries(LOAI).map(([v, l]) => (
                <option key={v} value={v}>
                  {l}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Năm</span>
            <input type="number" required min={2000} max={2100} value={meta.nam} onChange={set('nam')} />
          </label>
        </div>
        <div className="field-row">
          <label className="field">
            <span>Kỳ</span>
            <select value={meta.ky} onChange={set('ky')}>
              <option value="">Không có</option>
              {Object.entries(KY).map(([v, l]) => (
                <option key={v} value={v}>
                  {l}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Phạm vi</span>
            <select value={meta.phamVi} onChange={set('phamVi')}>
              <option value="">Không có</option>
              <option value="hop_nhat">Hợp nhất</option>
              <option value="rieng">Riêng</option>
            </select>
          </label>
        </div>
        <div className="field-row">
          <label className="field">
            <span>Ngày kết thúc kỳ</span>
            <input type="date" value={meta.ngayKetThucKy} onChange={set('ngayKetThucKy')} />
          </label>
          <label className="field">
            <span>Ngày ban hành</span>
            <input type="date" value={meta.ngayBanHanh} onChange={set('ngayBanHanh')} />
          </label>
        </div>
        {error && <div className="note warn">{error}</div>}
        <div className="dialog-actions">
          <button type="button" className="outline-btn compact" onClick={onClose}>
            Huỷ
          </button>
          <button type="submit" className="primary-btn compact" disabled={busy || !file}>
            {busy ? 'Đang tải lên…' : 'Tải lên'}
          </button>
        </div>
      </form>
    </div>
  )
}
