package com.vdt.finq.modules.rag.preprocessing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vdt.finq.modules.document.Document;
import com.vdt.finq.modules.rag.preprocessing.datacleaner.TextCleaner;
import com.vdt.finq.modules.rag.preprocessing.ocr.PageStore;

import lombok.RequiredArgsConstructor;

// đọc mọi trang OCR của một tài liệu và làm sạch; dùng chung cho lập chỉ mục và trích số
@Component
@RequiredArgsConstructor
public class DocumentPageReader {

    private final PageStore pageStore;
    private final TextCleaner textCleaner;

    // trang 1 ở vị trí 0; trang thiếu file hoặc trống thành chuỗi rỗng để giữ đúng số trang
    public List<String> readCleanPages(Document document) throws IOException {
        List<String> pages = new ArrayList<>(document.getTongSoTrang());
        for (int page = 1; page <= document.getTongSoTrang(); page++) {
            pages.add(pageStore.read(document.ocrRelativePath(), page).map(textCleaner::clean).orElse(""));
        }
        return pages;
    }
}
