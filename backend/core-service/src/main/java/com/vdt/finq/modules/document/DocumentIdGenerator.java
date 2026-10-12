package com.vdt.finq.modules.document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import com.vdt.finq.modules.document.DocumentEnums.PhamVi;

// sinh tai_lieu_id cùng dạng với devtools/quet_tai_lieu.py, ví dụ CTR-BCTC-HN-20231231-Q4-1a2b3c
public final class DocumentIdGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private DocumentIdGenerator() {
    }

    // cần đủ ma_cong_ty, loai, nam, duong_dan; pham_vi, ky, ngày có thì thêm vào
    public static String generate(Document document) {
        List<String> parts = new ArrayList<>();
        parts.add(document.getMaCongTy());
        parts.add(document.getLoai().getValue().toUpperCase().replace("_", ""));
        if (document.getPhamVi() != null) {
            parts.add(document.getPhamVi() == PhamVi.HOP_NHAT ? "HN" : "R");
        }
        if (document.getNgayKetThucKy() != null) {
            parts.add(document.getNgayKetThucKy().format(DATE));
        } else if (document.getNgayBanHanh() != null) {
            parts.add(document.getNgayBanHanh().format(DATE));
        } else {
            parts.add("NA" + document.getNam());
        }
        if (document.getKy() != null) {
            parts.add(document.getKy().getValue().toUpperCase());
        }
        // đuôi băm từ đường dẫn: ID không đổi khi thêm tài liệu khác
        parts.add(sha1(document.getDuongDan()).substring(0, 6));
        return String.join("-", parts);
    }

    private static String sha1(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // JVM nào cũng phải có SHA-1
            throw new IllegalStateException(e);
        }
    }
}
