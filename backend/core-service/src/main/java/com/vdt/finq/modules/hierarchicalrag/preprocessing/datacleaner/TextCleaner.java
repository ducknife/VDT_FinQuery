package com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

// làm sạch một trang trước khi chunk hoặc trích số liệu.
// KHÔNG BAO GIỜ ghi đè file .md trên đĩa: file gốc tốn một ngày mạng để tạo ra,
// nên làm sạch là phép biến đổi lúc đọc. đổi luật thì chỉ cần chạy lại bước chunk,
// không phải OCR lại

// các luật, theo đúng thứ tự chạy trong clean():
// 0. chuẩn hoá unicode NFC
// 1. bỏ hàng rào ``` bọc cả trang
// 2. &nbsp; thành dấu cách
// 3. gộp từ hai dấu cách liên tiếp trở lên giữa dòng thành một
// 4. bỏ khoảng trắng cuối dòng
// chi tiết và số đo: logs/lam-sach_v1.1.md
@Component
@RequiredArgsConstructor
public class TextCleaner {

    private static final String FENCE = "```"; // hàng rào code của markdown

    // chỉ gộp các dãy dấu cách đứng sau một ký tự khác trắng, thụt đầu dòng được giữ nguyên:
    // 1003 dòng trong kho thụt từ bốn dấu cách trở lên,
    // và trong markdown điều đó có nghĩa là một khối code
    private static final Pattern INNER_RUN = Pattern.compile("(?<=\\S)[ \\t]{2,}");

    // khoảng trắng cuối mỗi dòng
    private static final Pattern TRAILING = Pattern.compile("[ \\t]+$", Pattern.MULTILINE);

    private final NFCNormalization nfcNormalization;

    public String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        // thứ tự quan trọng: chuẩn hoá NFC trước để mọi luật sau chỉ gặp một kiểu mã hoá,
        // và &nbsp; phải thành dấu cách trước khi gộp các dãy dấu cách
        String text = nfcNormalization.toNfc(raw);
        text = stripCodeFence(text);
        text = text.replace("&nbsp;", " ");
        text = INNER_RUN.matcher(text).replaceAll(" ");
        text = TRAILING.matcher(text).replaceAll("");
        return text.strip();
    }

    // model đôi khi bọc cả trang trong ``` hoặc ```markdown.
    // kho còn 76 trang như vậy, công cụ node cũ bỏ sót dạng có nhãn
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
