package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.strategy;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.DocumentOcrResult;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.DocumentOcrService;

import lombok.RequiredArgsConstructor;

// pdf là định dạng duy nhất cần OCR: đọc lớp chữ thì mất bố cục trang,
// nên ta chuyển thành ảnh rồi để model thị giác đọc bảng
@Component
@RequiredArgsConstructor
public class PdfDocumentExtractor implements DocumentExtractor {

    private final DocumentOcrService documentOcrService;

    @Override
    public boolean supports(String fileName) {
        return fileName.toLowerCase().endsWith(".pdf");
    }

    @Override
    public DocumentOcrResult extract(Path file, String relativePath) throws IOException {
        return documentOcrService.ocrDocument(file, relativePath);
    }
}
