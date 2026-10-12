package com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

// làm sạch một trang lúc đọc, không ghi đè file .md (đổi luật chỉ cần chunk lại, không OCR lại)

// luật theo thứ tự: NFC, bỏ ``` bọc trang, &nbsp; thành dấu cách, gộp dấu cách giữa dòng, bỏ trắng cuối dòng.
// chi tiết: .docs/projectdevlogs/lam-sach_v1.1.md
@Component
@RequiredArgsConstructor
public class TextCleaner {

    private static final String FENCE = "```"; // hàng rào code của markdown

    // chỉ gộp dấu cách sau ký tự khác trắng; giữ thụt đầu dòng
    private static final Pattern INNER_RUN = Pattern.compile("(?<=\\S)[ \\t]{2,}");

    // khoảng trắng cuối mỗi dòng
    private static final Pattern TRAILING = Pattern.compile("[ \\t]+$", Pattern.MULTILINE);

    private final NFCNormalization nfcNormalization;

    public String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        // thứ tự: NFC trước, &nbsp; thành dấu cách trước khi gộp
        String text = nfcNormalization.toNfc(raw);
        text = stripCodeFence(text);
        text = text.replace("&nbsp;", " ");
        text = INNER_RUN.matcher(text).replaceAll(" ");
        text = TRAILING.matcher(text).replaceAll("");
        return text.strip();
    }

    // model đôi khi bọc cả trang trong ``` hoặc ```markdown
    private static String stripCodeFence(String text) {
        String trimmed = text.strip();
        if (!trimmed.startsWith(FENCE)) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline < 0) {
            // chỉ có hàng rào, không có nội dung
            return "";
        }
        // bỏ dòng mở đầu, có thể là ``` hoặc ```markdown
        String body = trimmed.substring(firstNewline + 1).stripTrailing();
        if (body.endsWith(FENCE)) {
            body = body.substring(0, body.length() - FENCE.length());
        }
        return body.strip();
    }
}
