package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// tóm tắt kết quả OCR một tài liệu
@Getter
@ToString
@AllArgsConstructor
public class DocumentOcrResult {

    private final int totalPages;
    private final int skipped;
    private final int succeeded;
    private final int failed;

    // chỉ đúng khi không còn trang nào phải làm, dùng để quyết định có ghi documents.ngay_ocr không
    public boolean isComplete() {
        return failed == 0;
    }
}
