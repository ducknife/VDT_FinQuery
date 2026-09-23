// OCR Studio — may chu nho, khong phu thuoc goi npm nao.
//   node server.mjs            -> mo tai http://localhost:5179
// Doc khoa tu D:/VDT_FinQuery/.env (bien LLM_API_KEY).

import http from "node:http";
import fs from "node:fs";
import fsp from "node:fs/promises";
import path from "node:path";
import url from "node:url";
import { spawn } from "node:child_process";

const HERE = path.dirname(url.fileURLToPath(import.meta.url));
const GOC = path.resolve(HERE, "..", "..");              // D:/VDT_FinQuery
const RAW = path.join(GOC, "data", "raw", "CTR");
const OCR = path.join(GOC, "data", "ocr", "CTR");
const CHI_MUC = path.join(GOC, "data", "ocr", "_chi_muc.json");
const CONG = 5179;

const API = "https://viettelai.vn/model-as-a-service/v1/chat/completions";

const CAU_HINH = {
  model: "Qwen3-VL-32B-Instruct-FP8",
  dpi: 170,
  luong: 6,
  maxTokens: 6000,
  chatLuong: 85,          // chat luong JPEG
  hanDau: 120,            // giay: han cho MANH CHU DAU TIEN (gom tai anh len + mo hinh doc anh)
  hanIm: 30,              // giay: han IM LANG giua hai manh — do duoc khoang lang dai nhat chi 4,4s
  hanTong: 900,           // giay: chan tren tuyet doi, phong xa
  canhBao: 60,            // giay: cho lau hon nay thi bao "cham" ra log
  nhip: 10,               // giay: nhip bao trang thai khi khong co gi xay ra
  boQuaTiengAnh: true,
  soLanToiDa: 6,          // thu toi 6 lan moi bo cuoc, va ghi vao danh sach "can xem tay"
  thangDpi: [0, 0, 130, 130, 110, 110],   // lan 3-4 ha 130 DPI, lan 5-6 ha 110 — da do la de qua hon
};

const NHAC = [
  "Trich xuat toan bo noi dung trang nay thanh Markdown.",
  "- Moi bang phai xuat ra dang bang Markdown, moi cot mot cot rieng biet.",
  "- Giu nguyen ma so chi tieu, so thuyet minh va DAY DU tung chu so cua moi con so.",
  "- So am giu nguyen dau ngoac don.",
  "- O trong thi de trong, khong dien 0.",
  "- Chi tra ve noi dung trang, khong giai thich gi them.",
].join("\n");

// ---------------------------------------------------------------- khoa API
function docKhoa() {
  try {
    const t = fs.readFileSync(path.join(GOC, ".env"), "utf8");
    const d = t.split(/\r?\n/).find((l) => l.startsWith("LLM_API_KEY"));
    return d ? d.split("=").slice(1).join("=").trim() : null;
  } catch {
    return null;
  }
}
const KHOA = docKhoa();

// ---------------------------------------------------------- tien trinh ve
let ve = null, veCho = new Map(), veId = 0, veDem = "";
function moVe() {
  ve = spawn("python", [path.join(HERE, "render_worker.py")], {
    cwd: GOC,
    env: { ...process.env, PYTHONIOENCODING: "utf-8" },
  });
  ve.stdout.setEncoding("utf8");
  ve.stdout.on("data", (c) => {
    veDem += c;
    let i;
    while ((i = veDem.indexOf("\n")) >= 0) {
      const dong = veDem.slice(0, i).trim();
      veDem = veDem.slice(i + 1);
      if (!dong) continue;
      try {
        const kq = JSON.parse(dong);
        const cb = veCho.get(kq.id);
        if (cb) { veCho.delete(kq.id); cb(kq); }
      } catch { /* bo qua dong hong */ }
    }
  });
  ve.stderr.on("data", (c) => phat("loi", { thong_diep: "render: " + String(c).slice(0, 200) }));
  ve.on("exit", () => { ve = null; });
}
function goiVe(lenh) {
  if (!ve) moVe();
  return new Promise((ok, that) => {
    const id = ++veId;
    veCho.set(id, (kq) => (kq.ok ? ok(kq) : that(new Error(kq.loi || "loi render"))));
    ve.stdin.write(JSON.stringify({ id, ...lenh }) + "\n");
    setTimeout(() => {
      if (veCho.has(id)) { veCho.delete(id); that(new Error("render qua han")); }
    }, 120000);
  });
}

// ------------------------------------------------------------------- SSE
const khach = new Set();
function phat(loai, du_lieu) {
  const goi = `data: ${JSON.stringify({ loai, ...du_lieu, luc: Date.now() })}\n\n`;
  for (const r of khach) { try { r.write(goi); } catch { /**/ } }
}

// -------------------------------------------------------------- chi muc
async function quet() {
  const ds = [];
  async function di(thu_muc) {
    for (const m of await fsp.readdir(thu_muc, { withFileTypes: true })) {
      const p = path.join(thu_muc, m.name);
      if (m.isDirectory()) await di(p);
      else if (m.name.toLowerCase().endsWith(".pdf")) ds.push(p);
    }
  }
  await di(RAW);
  ds.sort();

  let cu = {};
  try { cu = JSON.parse(await fsp.readFile(CHI_MUC, "utf8")); } catch { /**/ }

  const tai_lieu = [];
  for (const p of ds) {
    const tuong_doi = path.relative(RAW, p).replace(/\\/g, "/");
    const khoa = tuong_doi;
    let so_trang = cu[khoa]?.so_trang;
    let kytu = cu[khoa]?.kytu;
    if (!so_trang) {
      try {
        const r = await goiVe({ lenh: "sotrang", tep: p });
        so_trang = r.sotrang; kytu = r.kytu_12trang_dau;
      } catch { so_trang = 0; kytu = 0; }
    }
    const ra = path.join(OCR, tuong_doi.replace(/\.pdf$/i, ""));
    let xong = 0, en = 0;
    try {
      for (const f of await fsp.readdir(ra)) {
        if (/^trang_\d+\.md$/.test(f)) {
          const st = await fsp.stat(path.join(ra, f));
          if (st.size > 40) xong++;
        }
        if (f.endsWith(".en")) en++;
      }
    } catch { /* chua co thu muc */ }
    tai_lieu.push({
      khoa, tep: p, thu_muc: tuong_doi.split("/")[0], ten: path.basename(p),
      so_trang, kytu, xong, en, ra,
      xong_het: so_trang > 0 && xong >= so_trang,
    });
    cu[khoa] = { so_trang, kytu };
  }
  await fsp.mkdir(path.dirname(CHI_MUC), { recursive: true });
  await fsp.writeFile(CHI_MUC, JSON.stringify(cu, null, 1));
  return tai_lieu;
}


// Model hay boc ca ket qua trong ```markdown ... ``` — go bo de tep sach.
// Chi go khi rao BAO TRON ca noi dung, khong dung toi cac khoi ma nam giua trang.
function boRaoMa(t) {
  const dong = t.trim().split(/\r?\n/);
  if (dong.length >= 2 &&
      /^```[a-zA-Z]*\s*$/.test(dong[0]) &&
      /^```\s*$/.test(dong[dong.length - 1])) {
    return dong.slice(1, -1).join("\n").trim() + "\n";
  }
  return t.trim() + "\n";
}

// --------------------------------------------------- so lan hong cua tung trang
// Ghi ra dia (trang_XXX.loi) de tat may bat lai van nho da thu may lan.
function duongLoi(tl, i) {
  return path.join(tl.ra, `trang_${String(i + 1).padStart(3, "0")}.md.loi`);
}
async function docLan(tl, i) {
  try { return JSON.parse(await fsp.readFile(duongLoi(tl, i), "utf8")).lan || 0; }
  catch { return 0; }
}
async function ghiLan(tl, i, lan, loi) {
  try {
    await fsp.mkdir(tl.ra, { recursive: true });
    await fsp.writeFile(duongLoi(tl, i),
      JSON.stringify({ lan, luc: new Date().toISOString(), loi: String(loi).slice(0, 300) }, null, 1));
  } catch { /**/ }
}
async function xoaLoi(tl, i) { try { await fsp.unlink(duongLoi(tl, i)); } catch { /**/ } }

// ------------------------------------------------------------- goi OCR
const XUONG_DONG = String.fromCharCode(10);
const DAU_VIET = /[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ]/gi;

async function ocrMotTrang(tl, i, ghiTien, dpiRieng) {
  const dich = path.join(tl.ra, `trang_${String(i + 1).padStart(3, "0")}.md`);
  try { if ((await fsp.stat(dich)).size > 40) return { bo_qua: true }; } catch { /**/ }

  const anh = await goiVe({ lenh: "anh", tep: tl.tep, trang: i, dpi: dpiRieng || CAU_HINH.dpi, chat_luong: CAU_HINH.chatLuong });
  const than = {
    model: CAU_HINH.model, max_tokens: CAU_HINH.maxTokens, temperature: 0,
    messages: [{ role: "user", content: [
      { type: "text", text: NHAC },
      { type: "image_url", image_url: { url: "data:" + (anh.kieu || "image/jpeg") + ";base64," + anh.b64 } },
    ] }],
  };
  let cuoi;
  for (let lan = 0; lan < 2; lan++) {
    const t0 = Date.now();
    let noi = "", manh = 0, lan_cuoi = Date.now();
    const ctl = new AbortController();
    const thoi = (ly_do) => ctl.abort(new Error(ly_do));
    const bo_theo = () => thoi("da dung theo yeu cau");
    bo_ngang.signal.addEventListener("abort", bo_theo);

    // Canh: chi cat khi THUC SU im lang, khong cat vi trang dai.
    const canh = setInterval(() => {
      const im = (Date.now() - lan_cuoi) / 1000;
      const han = manh ? CAU_HINH.hanIm : CAU_HINH.hanDau;
      if (im > han) thoi(manh ? `im lang ${im.toFixed(0)}s sau ${manh} manh` : `khong co manh nao sau ${im.toFixed(0)}s`);
      else if ((Date.now() - t0) / 1000 > CAU_HINH.hanTong) thoi("vuot chan tren " + CAU_HINH.hanTong + "s");
    }, 2000);

    try {
      const r = await fetch(API, {
        method: "POST",
        headers: { Authorization: "Bearer " + KHOA, "Content-Type": "application/json" },
        body: JSON.stringify({ ...than, stream: true }),
        signal: ctl.signal,
      });
      if (!r.ok) throw new Error("HTTP " + r.status + " " + (await r.text()).slice(0, 120));

      const doc = r.body.getReader();
      const giai = new TextDecoder();
      let dem = "";
      for (;;) {
        const { done, value } = await doc.read();
        if (done) break;
        dem += giai.decode(value, { stream: true });
        let i;
        while ((i = dem.indexOf(XUONG_DONG)) >= 0) {
          const dong = dem.slice(0, i).trim();
          dem = dem.slice(i + 1);
          if (!dong.startsWith("data:")) continue;
          const than_dong = dong.slice(5).trim();
          if (than_dong === "[DONE]") continue;
          let j;
          try { j = JSON.parse(than_dong); } catch { continue; }
          const c = j.choices?.[0]?.delta?.content;
          if (c) { noi += c; manh++; lan_cuoi = Date.now(); if (ghiTien) ghiTien(manh); }
        }
      }
      clearInterval(canh);
      bo_ngang.signal.removeEventListener("abort", bo_theo);
      if (!noi.trim()) throw new Error("tra ve rong");

      noi = boRaoMa(noi);
      const tok = Math.round(noi.length / 1.46);   // uoc tu so ky tu: che do chay khong tra usage
      await fsp.mkdir(tl.ra, { recursive: true });
      await fsp.writeFile(dich, noi, "utf8");

      const dau = (noi.match(DAU_VIET) || []).length;
      const la_anh = noi.length > 400 && dau / noi.length < 0.005;
      if (la_anh && CAU_HINH.boQuaTiengAnh) await fsp.writeFile(dich + ".en", "1");
      thong_ke.byte += (anh.byte || 0) * (lan + 1);
      await xoaLoi(tl, i);          // xong thi xoa dau vet hong
      return { tok, so_ky_tu: noi.length, la_anh, manh };
    } catch (e) {
      clearInterval(canh);
      bo_ngang.signal.removeEventListener("abort", bo_theo);
      cuoi = new Error((ctl.signal.reason?.message || e.message || String(e)) +
                       ` (da nhan ${manh} manh trong ${((Date.now() - t0) / 1000).toFixed(0)}s)`);
      if (dung_lai) throw new Error("da dung theo yeu cau");
      await new Promise((r) => setTimeout(r, 4000 * (lan + 1)));
    }
  }
  throw cuoi;
}

// ------------------------------------------------------------ dieu phoi
let dang_chay = false, dung_lai = false, bo_ngang = null;
const dang_bay = new Map();      // so theo doi: trang nao dang cho, cho tu bao gio
let dem_bay = 0, hen_nhip = null;

// Nhip: cu vai giay bao mot lan xem con may trang dang cho va cho bao lau roi.
// Muc dich: nhin vao log la biet ngay "dang cham" hay "da chet".
function batNhip() {
  dung(); // don nhip cu neu con
  hen_nhip = setInterval(() => {
    if (!dang_bay.size) return;
    const gio = Date.now();
    const ds = [...dang_bay.values()];
    const cu_nhat = ds.reduce((a, b) => (a.luc <= b.luc ? a : b));
    const lau_nhat = (gio - cu_nhat.luc) / 1000;
    for (const x of ds) {
      const cho = (gio - x.luc) / 1000;
      if (cho >= CAU_HINH.canhBao && !x.da_bao) {
        x.da_bao = true;
        phat("cham", { ten: x.ten, trang: x.trang, giay: cho, manh: x.manh || 0 });
      }
    }
    phat("nhip", { dang_cho: dang_bay.size, lau_nhat, manh: cu_nhat.manh || 0,
                   ten: cu_nhat.ten, trang: cu_nhat.trang,
                   tong_trang: thong_ke.trang, tong_token: thong_ke.token });
  }, CAU_HINH.nhip * 1000);
  function dung() { if (hen_nhip) { clearInterval(hen_nhip); hen_nhip = null; } }
}
function tatNhip() { if (hen_nhip) { clearInterval(hen_nhip); hen_nhip = null; } }
const thong_ke = { trang: 0, token: 0, loi: 0, bat_dau: 0, tep_xong: 0, byte: 0, loi_lien_tiep: 0 };

// Chay mot me trang cua mot tai lieu, nhieu luong.
// Trang hong duoc ghi so lan ra dia; lan sau se ha DPI cho de qua hon.
async function chayMe(tl, can) {
  let vi_tri = 0;
  const tho = Array.from({ length: Math.min(CAU_HINH.luong, can.length) }, async () => {
    while (!dung_lai) {
      const i = vi_tri++;
      if (i >= can.length) break;
      const tr = can[i];
      const lan_truoc = await docLan(tl, tr);
      if (lan_truoc >= CAU_HINH.soLanToiDa) {
        phat("trang", { ten: tl.ten, trang: tr + 1, trang_thai: "bo_han", lan: lan_truoc });
        continue;
      }
      const dpi = CAU_HINH.thangDpi[lan_truoc] || CAU_HINH.dpi;
      const t0 = Date.now();
      const the = ++dem_bay;
      dang_bay.set(the, { ten: tl.ten, trang: tr + 1, luc: t0 });
      try {
        const kq = await ocrMotTrang(tl, tr, (m) => { const x = dang_bay.get(the); if (x) x.manh = m; }, dpi);
        dang_bay.delete(the);
        if (kq.bo_qua) { phat("trang", { ten: tl.ten, trang: tr + 1, trang_thai: "co_san" }); continue; }
        thong_ke.trang++; thong_ke.token += kq.tok; thong_ke.loi_lien_tiep = 0;
        phat("trang", {
          ten: tl.ten, trang: tr + 1, trang_thai: kq.la_anh ? "tieng_anh" : "ok",
          giay: (Date.now() - t0) / 1000, token: kq.tok, so_ky_tu: kq.so_ky_tu, dpi,
          lan: lan_truoc + 1,
          tong_trang: thong_ke.trang, tong_token: thong_ke.token, tong_byte: thong_ke.byte,
        });
      } catch (e) {
        dang_bay.delete(the);
        if (dung_lai) { phat("trang", { ten: tl.ten, trang: tr + 1, trang_thai: "huy" }); break; }
        const lan = lan_truoc + 1;
        await ghiLan(tl, tr, lan, e);
        thong_ke.loi++; thong_ke.loi_lien_tiep++;
        const qua_han = /timeout|abort|im lang|khong co manh/i.test(String(e));
        phat("trang", {
          ten: tl.ten, trang: tr + 1, trang_thai: qua_han ? "qua_han" : "loi",
          giay: (Date.now() - t0) / 1000, lan, con_lai: CAU_HINH.soLanToiDa - lan, dpi,
          thong_diep: String(e.message || e).slice(0, 160),
        });
        if (thong_ke.loi_lien_tiep >= 8) {
          dung_lai = true; if (bo_ngang) bo_ngang.abort();
          phat("loi", { thong_diep: "8 trang hong lien tiep — tu dung de khoi ton bang thong. Kiem tra mang roi chay lai." });
        }
      }
    }
  });
  await Promise.all(tho);
}

// Pha VET: di het hang doi roi quay lai lam sach nhung trang con hong.
// Lap cho toi khi khong con trang nao thieu, hoac tat ca deu da qua soLanToiDa.
async function vet(tat_ca, loc) {
  for (let vong = 1; vong <= 4 && !dung_lai; vong++) {
    const viec = [];
    for (const tl of tat_ca) {
      if (loc?.thu_muc?.length && !loc.thu_muc.includes(tl.thu_muc)) continue;
      if (!tl.so_trang) continue;
      const thieu = [];
      for (let i = 0; i < tl.so_trang; i++) {
        const dich = path.join(tl.ra, `trang_${String(i + 1).padStart(3, "0")}.md`);
        let co = false;
        try { co = (await fsp.stat(dich)).size > 40; } catch { /**/ }
        if (co) continue;
        if ((await docLan(tl, i)) >= CAU_HINH.soLanToiDa) continue;   // da bo cuoc
        thieu.push(i);
      }
      if (thieu.length) viec.push([tl, thieu]);
    }
    const tong = viec.reduce((a, b) => a + b[1].length, 0);
    if (!tong) { phat("vet_xong", { vong }); return; }

    phat("vet", { vong, so_tep: viec.length, so_trang: tong });
    for (const [tl, thieu] of viec) {
      if (dung_lai) break;
      phat("mo_tep", { ten: tl.ten, thu_muc: tl.thu_muc, so_trang: tl.so_trang, da_co: tl.so_trang - thieu.length, vet: true });
      await chayMe(tl, thieu);
      await gopToanVan(tl);
    }
    if (!dung_lai && vong < 4) await new Promise((r) => setTimeout(r, 15000 * vong));  // nghi giua cac vong
  }
  // con gi khong xong thi liet ke ra
  const con = [];
  for (const tl of tat_ca) {
    if (!tl.so_trang) continue;
    for (let i = 0; i < tl.so_trang; i++) {
      const dich = path.join(tl.ra, `trang_${String(i + 1).padStart(3, "0")}.md`);
      try { if ((await fsp.stat(dich)).size > 40) continue; } catch { /**/ }
      const lan = await docLan(tl, i);
      if (lan) con.push({ ten: tl.ten, trang: i + 1, lan });
    }
  }
  if (con.length) phat("can_xem_tay", { so_trang: con.length, ds: con.slice(0, 40) });
}

async function chay(loc) {
  if (dang_chay) return;
  if (!KHOA) { phat("loi", { thong_diep: "Khong tim thay LLM_API_KEY trong .env" }); return; }
  dang_chay = true; dung_lai = false; bo_ngang = new AbortController();
  Object.assign(thong_ke, { trang: 0, token: 0, loi: 0, bat_dau: Date.now(), tep_xong: 0, byte: 0, loi_lien_tiep: 0 });
  batNhip();
  phat("batdau", { cau_hinh: CAU_HINH });

  const tat_ca = await quet();
  const viec = tat_ca.filter((t) => !t.xong_het && t.so_trang > 0 &&
    (!loc?.thu_muc?.length || loc.thu_muc.includes(t.thu_muc)));
  phat("hang_doi", { so_tep: viec.length, so_trang: viec.reduce((a, b) => a + (b.so_trang - b.xong), 0) });

  for (const tl of viec) {
    if (dung_lai) break;
    phat("mo_tep", { ten: tl.ten, thu_muc: tl.thu_muc, so_trang: tl.so_trang, da_co: tl.xong });
    const can = [];
    for (let i = 0; i < tl.so_trang; i++) can.push(i);

    await chayMe(tl, can);
    if (!dung_lai) { thong_ke.tep_xong++; await gopToanVan(tl); phat("xong_tep", { ten: tl.ten, tep_xong: thong_ke.tep_xong }); }
  }
  if (!dung_lai) await vet(tat_ca, loc);
  dang_chay = false; tatNhip(); dang_bay.clear();
  phat("ket_thuc", { ...thong_ke, phut: (Date.now() - thong_ke.bat_dau) / 60000 });
}

async function gopToanVan(tl) {
  try {
    const ds = (await fsp.readdir(tl.ra)).filter((f) => /^trang_\d+\.md$/.test(f)).sort();
    let g = "";
    for (const f of ds) {
      if (fs.existsSync(path.join(tl.ra, f + ".en"))) continue;   // bo phan tieng Anh
      g += `\n\n<!-- ===== ${f.replace(/\D/g, "")} ===== -->\n\n` + await fsp.readFile(path.join(tl.ra, f), "utf8");
    }
    await fsp.writeFile(path.join(tl.ra, "_TOAN_VAN.md"), g, "utf8");
  } catch { /**/ }
}

// ------------------------------------------------------------------- HTTP
const KIEU = { ".html": "text/html; charset=utf-8", ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8", ".json": "application/json; charset=utf-8" };

const may = http.createServer(async (req, res) => {
  const u = new URL(req.url, "http://x");

  if (u.pathname === "/api/sukien") {
    res.writeHead(200, { "Content-Type": "text/event-stream", "Cache-Control": "no-cache", Connection: "keep-alive" });
    res.write(": xin chao\n\n");
    khach.add(res);
    req.on("close", () => khach.delete(res));
    return;
  }

  if (u.pathname === "/api/trangthai") {
    const tl = await quet();
    const theo_tm = {};
    for (const t of tl) {
      const k = theo_tm[t.thu_muc] ||= { thu_muc: t.thu_muc, tep: 0, trang: 0, xong: 0, en: 0 };
      k.tep++; k.trang += t.so_trang; k.xong += t.xong; k.en += t.en;
    }
    res.writeHead(200, KIEU[".json"]).end(JSON.stringify({
      dang_chay, cau_hinh: CAU_HINH, thong_ke, co_khoa: !!KHOA,
      thu_muc: Object.values(theo_tm),
      tai_lieu: tl.map(({ tep, ra, ...r }) => r),
    }));
    return;
  }

  if (u.pathname === "/api/batdau" && req.method === "POST") {
    const than = await doc(req);
    Object.assign(CAU_HINH, than?.cau_hinh || {});
    chay(than?.loc);
    res.writeHead(200, KIEU[".json"]).end(JSON.stringify({ ok: true }));
    return;
  }

  if (u.pathname === "/api/dung" && req.method === "POST") {
    dung_lai = true;
    if (bo_ngang) bo_ngang.abort();          // cat dut cac yeu cau dang bay
    phat("dang_dung", {});
    res.writeHead(200, KIEU[".json"]).end(JSON.stringify({ ok: true, dang_chay }));
    return;
  }

  // tinh
  let p = u.pathname === "/" ? "/index.html" : u.pathname;
  const tep = path.join(HERE, "public", p);
  if (!tep.startsWith(path.join(HERE, "public"))) { res.writeHead(403).end(); return; }
  try {
    const n = await fsp.readFile(tep);
    res.writeHead(200, { "Content-Type": KIEU[path.extname(tep)] || "application/octet-stream" }).end(n);
  } catch {
    res.writeHead(404).end("khong thay: " + p);
  }
});

function doc(req) {
  return new Promise((ok) => {
    let s = "";
    req.on("data", (c) => (s += c));
    req.on("end", () => { try { ok(JSON.parse(s || "{}")); } catch { ok({}); } });
  });
}

may.listen(CONG, () => {
  console.log(`
  ╔══════════════════════════════════════════════╗
  ║   OCR STUDIO  ·  FinQuery                    ║
  ║   http://localhost:${CONG}                      ║
  ╚══════════════════════════════════════════════╝
  khoa API : ${KHOA ? "da doc tu .env" : "!! THIEU trong .env !!"}
  kho goc  : ${RAW}
  ket qua  : ${OCR}
`);
});
