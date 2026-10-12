package com.vdt.finq.common.markdown;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

// bảng markdown đã tách ô: headerRows trước dòng kẻ |---|, bodyRows sau dòng kẻ
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
