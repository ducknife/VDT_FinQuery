// OCR Studio — giao dien React, nap thang tu CDN, khong can buoc build.
import React from "https://esm.sh/react@18.3.1";
import { createRoot } from "https://esm.sh/react-dom@18.3.1/client";
import htm from "https://esm.sh/htm@3.1.1";

const { useState, useEffect, useRef, useMemo } = React;
const h = React.createElement;
const html = htm.bind(h);

const so = (n) => (n ?? 0).toLocaleString("vi-VN");
const gio = (t) => new Date(t).toLocaleTimeString("vi-VN", { hour12: false });
const BANG = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+/";

function thanhVach(ty_le, rong = 34) {
  const day = Math.round(Math.max(0, Math.min(1, ty_le)) * rong);
  return { day: "█".repeat(day), trong: "░".repeat(rong - day) };
}

const CAU_VUI = [
  "Đang bắt từng con số bỏ vào rọ...",
  "Thuyết minh dài quá, hít một hơi...",
  "Mã chỉ tiêu ơi, đứng yên nào!",
  "Charmander hơ lửa cho chữ khô...",
  "Bảng cân đối phải cân, không cãi.",
  "Trang này scan hơi nghiêng, ráng đọc...",
  "Dấu tiếng Việt đã được cứu.",
  "Số âm nằm trong ngoặc, đã ghi nhận.",
  "Đừng lo, token còn nhiều lắm.",
  "Tool OCR: Create By Ducknife",
  "Một trang nữa thôi... ờ, còn vài nghìn.",
  "Poké Ball đang quay, đừng chớp mắt.",
];

// nguoi nhay — 4 khung
const NHAY = ["♪┏(・o・)┛♪", "♪┗(・o・)┓♪", "♪┏(・o・)┛♪", "♪┗(・o・)┓♪"];
const NGU = "(-_-) zZ";

function Khung({ phim, nhanh, nho, nen, tran, lech = 0 }) {
  const [i, datI] = useState(0);
  useEffect(() => {
    if (!phim) return;
    datI(0);
    const nhip = 1000 / (phim.fps * (nhanh ? 1.4 : 1));
    const t = setInterval(() => datI((v) => (v + 1) % phim.khung.length), nhip);
    return () => clearInterval(t);
  }, [phim, nhanh]);

  if (!phim) return null;
  const k = phim.khung[(i + lech) % phim.khung.length];
  const hang = [];
  for (let y = 0; y < k.k.length; y++) {
    const o = [];
    let dem = "", mau = null;
    for (let x = 0; x < k.k[y].length; x++) {
      const m = k.m[y][x] === " " ? null : phim.bangMau[BANG.indexOf(k.m[y][x])];
      if (m !== mau) {
        if (dem) o.push(h("span", { key: o.length, style: { color: mau || "transparent" } }, dem));
        dem = ""; mau = m;
      }
      dem += k.k[y][x];
    }
    if (dem) o.push(h("span", { key: o.length, style: { color: mau || "transparent" } }, dem));
    hang.push(h("div", { key: y }, o));
  }
  if (nen || tran) return html`<pre>${hang}</pre>`;
  return html`
    <div className=${"san" + (nho ? " nho" : "")} style=${{ background: phim.nen }}>
      <span className="nhan-san">${phim.ten}</span>
      <pre>${hang}</pre>
    </div>`;
}

function Ung() {
  const [hu, datHu] = useState(null);
  const [tt, datTt] = useState(null);
  const [log, datLog] = useState([]);
  const [chay, datChay] = useState(false);
  const [tm_chon, datTmChon] = useState("tat_ca");
  const [luong, datLuong] = useState(6);
  const [dpi, datDpi] = useState(170);
  const [cau, datCau] = useState(CAU_VUI[0]);
  const [nhay_i, datNhayI] = useState(0);
  const [dang_dung, datDangDung] = useState(false);
  const [nhip, datNhip] = useState(null);
  const [hien_tai, datHienTai] = useState(null);
  const may = useRef(null);

  const ghi = (kieu, van) =>
    datLog((l) => [...l.slice(-400), { kieu, van, luc: Date.now(), id: Math.random() }]);

  const napTT = () => fetch("/api/trangthai").then((r) => r.json()).then((d) => {
    datTt(d); datChay(d.dang_chay);
  });

  useEffect(() => {
    napTT();
    fetch("./hieu_ung.json").then((r) => r.json()).then(datHu).catch(() => {});
  }, []);

  useEffect(() => {
    const es = new EventSource("/api/sukien");
    es.onmessage = (e) => {
      const s = JSON.parse(e.data);
      if (s.loai === "dang_dung") { datDangDung(true); ghi("loi", "<< dang dung: da huy cac yeu cau dang cho..."); }
      if (s.loai === "batdau") { datChay(true); datDangDung(false); ghi("ok", ">> khởi động · " + s.cau_hinh.model + " · " + s.cau_hinh.luong + " luồng · " + s.cau_hinh.dpi + " DPI"); }
      if (s.loai === "hang_doi") ghi("nhat", "hàng đợi: " + s.so_tep + " tệp · " + so(s.so_trang) + " trang còn thiếu");
      if (s.loai === "mo_tep") { datHienTai({ ten: s.ten, so_trang: s.so_trang, xong: s.da_co }); ghi("tep", "[tệp] " + s.ten + "  (" + s.so_trang + " trang, đã có " + s.da_co + ")"); }
      if (s.loai === "trang") {
        if (s.trang_thai === "ok") { ghi("ok", "  + trang " + String(s.trang).padStart(3) + " · " + s.giay.toFixed(1) + "s · " + so(s.token) + " tok · " + so(s.so_ky_tu) + " ký tự" + (s.lan > 1 ? "  (lần " + s.lan + " · " + s.dpi + " DPI)" : "")); datHienTai((v) => v && { ...v, xong: v.xong + 1 }); }
        else if (s.trang_thai === "tieng_anh") { ghi("bo", "  ~ trang " + String(s.trang).padStart(3) + " · tiếng Anh, bỏ khỏi bản ghép"); datHienTai((v) => v && { ...v, xong: v.xong + 1 }); }
        else if (s.trang_thai === "huy") ghi("bo", "  x trang " + String(s.trang).padStart(3) + " · da huy");
        else if (s.trang_thai === "qua_han" || s.trang_thai === "loi")
          ghi("loi", "  ! trang " + String(s.trang).padStart(3) + " · lần " + s.lan + "/" + (s.lan + s.con_lai)
              + " · " + s.dpi + " DPI · " + s.thong_diep
              + (s.con_lai > 0 ? " — sẽ làm lại ở pha vét" : " — BỎ CUỘC"));
        else if (s.trang_thai === "bo_han") ghi("loi", "  x trang " + String(s.trang).padStart(3) + " · đã bỏ cuộc sau " + s.lan + " lần");
        if (s.tong_trang) datTt((d) => d && { ...d, thong_ke: { ...d.thong_ke, trang: s.tong_trang, token: s.tong_token } });
      }
      if (s.loai === "xong_tep") { ghi("tep", "[xong] " + s.ten + " — đã ghép _TOAN_VAN.md"); napTT(); }
      if (s.loai === "ket_thuc") { datChay(false); datDangDung(false); datHienTai(null); datNhip(null); ghi("tep", "[kết thúc] " + so(s.trang) + " trang · " + so(s.token) + " token · " + s.loi + " lỗi · " + s.phut.toFixed(1) + " phút"); napTT(); }
      if (s.loai === "vet") ghi("tep", "[VÉT vòng " + s.vong + "] " + s.so_tep + " tệp · " + s.so_trang + " trang còn thiếu — làm lại");
      if (s.loai === "vet_xong") ghi("ok", "[VÉT] không còn trang nào thiếu — sạch sau " + s.vong + " vòng");
      if (s.loai === "can_xem_tay") ghi("loi", "[CẦN XEM TAY] " + s.so_trang + " trang bỏ cuộc sau " + 6 + " lần: " + s.ds.map((x) => x.ten + " tr" + x.trang).slice(0, 8).join(", "));
      if (s.loai === "nhip") datNhip({ dang_cho: s.dang_cho, lau_nhat: s.lau_nhat, manh: s.manh });
      if (s.loai === "cham") ghi("cho", "  … trang " + String(s.trang).padStart(3) + " · chờ " + s.giay.toFixed(0) + "s · đã nhận " + s.manh + " mảnh chữ");
      if (s.loai === "loi") ghi("loi", "! " + s.thong_diep);
    };
    return () => es.close();
  }, []);

  useEffect(() => { if (may.current) may.current.scrollTop = may.current.scrollHeight; }, [log]);

  useEffect(() => {
    const a = setInterval(() => datCau(CAU_VUI[Math.floor(Math.random() * CAU_VUI.length)]), 6000);
    const b = setInterval(() => datNhayI((v) => (v + 1) % NHAY.length), 240);
    return () => { clearInterval(a); clearInterval(b); };
  }, []);

  const tong = useMemo(() => {
    const t = tt?.thu_muc || [];
    return {
      trang: t.reduce((a, b) => a + b.trang, 0),
      xong: t.reduce((a, b) => a + b.xong, 0),
      tep: t.reduce((a, b) => a + b.tep, 0),
    };
  }, [tt]);

  const tk = tt?.thong_ke || {};
  const phut = tk.bat_dau && chay ? (Date.now() - tk.bat_dau) / 60000 : 0;
  const toc = phut > 0 ? tk.trang / phut : 0;
  const con = tong.trang - tong.xong;
  const con_lai = toc > 0 ? con / toc : 0;

  const batDau = () => {
    datLog([]);
    fetch("/api/batdau", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        cau_hinh: { luong: Number(luong), dpi: Number(dpi) },
        loc: tm_chon === "tat_ca" ? {} : { thu_muc: [tm_chon] },
      }),
    });
  };
  const dung = () => { datDangDung(true); fetch("/api/dung", { method: "POST" }); };

  const v = thanhVach(tong.trang ? tong.xong / tong.trang : 0);
  const vh = hien_tai ? thanhVach(hien_tai.so_trang ? hien_tai.xong / hien_tai.so_trang : 0, 28) : null;

  return html`
    <header>
      <span className="hieu">OCR <b>STUDIO</b></span>
      <span className=${"chip " + (tt?.co_khoa ? "on" : "off")}>${tt?.co_khoa ? "có khoá API" : "thiếu khoá"}</span>
      <span className=${"chip " + (chay ? "on" : "")}>${dang_dung ? "đang dừng" : chay ? "đang chạy" : "nghỉ"}</span>
      <span className="day"></span>
      <label>thư mục
        <select value=${tm_chon} onChange=${(e) => datTmChon(e.target.value)} disabled=${chay}>
          <option value="tat_ca">tất cả</option>
          ${(tt?.thu_muc || []).map((t) => html`<option key=${t.thu_muc} value=${t.thu_muc}>${t.thu_muc}</option>`)}
        </select>
      </label>
      <label>luồng <input type="number" min="1" max="24" value=${luong} onChange=${(e) => datLuong(e.target.value)} disabled=${chay} style=${{ width: "56px" }} /></label>
      <label>DPI <input type="number" min="90" max="300" step="10" value=${dpi} onChange=${(e) => datDpi(e.target.value)} disabled=${chay} style=${{ width: "64px" }} /></label>
      ${chay
        ? html`<button className="dung" onClick=${dung} disabled=${dang_dung}>${dang_dung ? "ĐANG DỪNG…" : "DỪNG"}</button>`
        : html`<button className="chinh" onClick=${batDau} disabled=${!tt?.co_khoa}>CHẠY TIẾP</button>`}
    </header>

    <main>
      <div className="trai">
        <div className="luoi">
          <div className="the do"><div className="nhan">trang đã OCR</div><div className="so">${so(tong.xong)}</div></div>
          <div className="the"><div className="nhan">còn lại</div><div className="so">${so(con)}</div></div>
          <div className="the"><div className="nhan">token phiên này</div><div className="so">${so(tk.token)}</div></div>
          <div className="the"><div className="nhan">tốc độ</div><div className="so">${toc ? toc.toFixed(1) : "—"}<span style=${{ fontSize: "11px" }}> tr/ph</span></div></div>
        </div>

        <div className="thanh">
          <div className="dong"><span>toàn kho</span><span><b>${tong.xong}</b>/${tong.trang} trang · ${tong.tep} tệp</span></div>
          <div className="vach">${v.day}<span className="trong">${v.trong}</span></div>
        </div>

        ${hien_tai && html`
          <div className="thanh">
            <div className="dong"><b>${hien_tai.ten}</b><span>${hien_tai.xong}/${hien_tai.so_trang}</span></div>
            <div className="vach">${vh.day}<span className="trong">${vh.trong}</span></div>
          </div>`}

        ${chay && con_lai > 0 && html`<div className="uoc">còn khoảng ${con_lai < 60 ? con_lai.toFixed(0) + " phút" : (con_lai / 60).toFixed(1) + " giờ"} nữa</div>`}

        <table>
          <thead><tr><th>thư mục</th><th className="p">tệp</th><th className="p">trang</th><th className="p">xong</th><th className="p">%</th></tr></thead>
          <tbody>
            ${(tt?.thu_muc || []).map((t) => html`
              <tr key=${t.thu_muc}>
                <td>${t.thu_muc.replace(/^\d+_/, "")}</td>
                <td className="p">${t.tep}</td>
                <td className="p">${so(t.trang)}</td>
                <td className="p xong">${so(t.xong)}</td>
                <td className="p">${t.trang ? Math.round((t.xong / t.trang) * 100) : 0}%</td>
              </tr>`)}
          </tbody>
        </table>

        <div className="qua-cau">
          <${Khung} phim=${hu && hu.pokeball} nhanh=${chay} tran=${true} />
        </div>
      </div>

      <div className="phai">
        <div className="khung-log">
          <div className=${"goc-charmander" + (chay ? " dang" : "")}>
            <${Khung} phim=${hu && hu.charmander} nhanh=${chay} tran=${true} />
          </div>
          <div className="may" ref=${may}>
            ${log.length === 0 && html`<div className="nhat">chưa có gì. Bấm <b>CHẠY TIẾP</b> để OCR những trang còn thiếu — trang nào xong rồi thì tự bỏ qua.</div>`}
            ${log.map((d) => html`<div className=${"dong-log " + d.kieu} key=${d.id}><span className="gio">${gio(d.luc)}</span> ${d.van}</div>`)}
          </div>
        </div>
        <div className=${"dai-lua" + (chay ? " dang" : "")}>
          ${hu && [0, 5, 11, 18, 26, 34, 47, 58, 70, 83].map((L, n) => html`
            <${Khung} key=${n} phim=${hu.lua} nhanh=${chay} tran=${true} lech=${L} />`)}
        </div>
        <div className="chan">
          <span className=${"nguoi-nhay" + (chay ? " dang" : "")}>${chay ? NHAY[nhay_i] : NGU}</span>
          <span className="cau">${
            chay && nhip
              ? nhip.dang_cho + " trang đang chạy · lâu nhất " + nhip.lau_nhat.toFixed(0) + "s (" + (nhip.manh || 0) + " mảnh)"
              : chay ? cau : "nghỉ tay chút đã"}</span>
          <span className="may-chay">${(tt && tt.cau_hinh && tt.cau_hinh.model) || ""}</span>
        </div>
      </div>
    </main>`;
}

createRoot(document.getElementById("goc")).render(html`<${Ung} />`);
