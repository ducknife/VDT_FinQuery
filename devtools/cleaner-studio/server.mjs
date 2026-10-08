// server tĩnh nhỏ cho Cleaner Studio.
// nó còn chuyển tiếp /api/* sang ứng dụng Spring để trình duyệt không vướng CORS.
//
//   node devtools/cleaner-studio/server.mjs
//   http://localhost:5180
//
// ứng dụng Spring phải đang chạy với:  cleaner.test-api.enabled=true

import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const GOC = path.dirname(fileURLToPath(import.meta.url));
const CONG = 5180;
const SPRING = "http://localhost:8080";

const KIEU = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
};

const may = http.createServer(async (req, res) => {
  const duong = new URL(req.url, "http://x").pathname;

  // chuyển tiếp sang Spring, tránh CORS
  if (duong.startsWith("/api/")) {
    try {
      const dich = SPRING + duong.replace("/api", "/test/cleaner") + (req.url.split("?")[1] ? "?" + req.url.split("?")[1] : "");
      const r = await fetch(dich);
      const t = await r.text();
      res.writeHead(r.status, { "content-type": "application/json; charset=utf-8" });
      res.end(t);
    } catch (e) {
      res.writeHead(502, { "content-type": "application/json; charset=utf-8" });
      res.end(JSON.stringify({ loi: "khong goi duoc Spring o " + SPRING + " — da chay app chua?" }));
    }
    return;
  }

  const tep = path.join(GOC, duong === "/" ? "index.html" : duong.slice(1));
  if (!tep.startsWith(GOC) || !fs.existsSync(tep)) {
    res.writeHead(404).end("khong thay");
    return;
  }
  res.writeHead(200, { "content-type": KIEU[path.extname(tep)] || "text/plain" });
  res.end(fs.readFileSync(tep));
});

may.listen(CONG, () => {
  console.log("Cleaner Studio  ->  http://localhost:" + CONG);
  console.log("Chuyen tiep /api/*  ->  " + SPRING + "/test/cleaner/*");
});
