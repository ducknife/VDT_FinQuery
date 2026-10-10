package com.vdt.finq.common.markdown;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

// một bảng markdown đã tách ô. headerRows là các dòng trước dòng kẻ |---|,
// bodyRows là các dòng sau; bảng không có dòng kẻ thì headerRows rỗng
@Getter
@AllArgsConstructor
public class MarkdownTable {
    private final List<List<String>> headerRows;
    private final List<List<String>> bodyRows;
    // các dòng gốc của bảng, để trả lại nguyên văn khi không xử lý được
    private final List<String> lines;

    public boolean hasHeader() {
        return !headerRows.isEmpty();
    }
}
