package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.strategy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Service;

import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.DocumentOcrResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// chọn bộ trích xuất cho một file. spring tiêm mọi implementation vào,
// nên thêm định dạng mới không phải sửa class này
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionService {

    private final List<DocumentExtractor> extractors;

    public DocumentOcrResult extract(Path file, String relativePath) throws IOException {
        String fileName = file.getFileName().toString();
        DocumentExtractor extractor = extractors.stream()
                .filter(e -> e.supports(fileName))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        "no extractor for " + fileName));

        log.info("{} -> {}", fileName, extractor.getClass().getSimpleName());
        return extractor.extract(file, relativePath);
    }

    // endpoint tải lên dùng hàm này để từ chối file trước khi lưu
    public boolean canExtract(String fileName) {
        return extractors.stream().anyMatch(e -> e.supports(fileName));
    }
}
