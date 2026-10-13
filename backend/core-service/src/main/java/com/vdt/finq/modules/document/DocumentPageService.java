package com.vdt.finq.modules.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.properties.IngestionProperties;
import com.vdt.finq.modules.rag.preprocessing.ocr.PageStore;
import com.vdt.finq.modules.rag.preprocessing.ocr.PdfRenderer;

import lombok.RequiredArgsConstructor;

// xem trang được trích dẫn: ảnh trang PDF gốc và chữ OCR của trang đó
@Service
@RequiredArgsConstructor
public class DocumentPageService {

    private static final int PREVIEW_DPI = 110;
    private static final float JPEG_QUALITY = 0.8f;

    private final DocumentRepository repository;
    private final IngestionProperties ingestionProperties;
    private final PageStore pageStore;

    public byte[] pageImage(String taiLieuId, int trang) {
        Document doc = find(taiLieuId, trang);
        Path pdf = Path.of(ingestionProperties.getRawDir()).toAbsolutePath().normalize()
                .resolve(doc.getMaCongTy()).resolve(doc.getDuongDan() + ".pdf").normalize();
        if (!Files.isRegularFile(pdf)) {
            throw new AppException(404, "không có file PDF gốc của " + taiLieuId);
        }
        try (PdfRenderer renderer = PdfRenderer.open(pdf)) {
            return renderer.render(trang - 1, PREVIEW_DPI, JPEG_QUALITY);
        } catch (IOException | IllegalArgumentException e) {
            throw new AppException(404, "không dựng được trang " + trang + " của " + taiLieuId + ": " + e.getMessage());
        }
    }

    public String pageText(String taiLieuId, int trang) {
        Document doc = find(taiLieuId, trang);
        try {
            return pageStore.read(doc.ocrRelativePath(), trang)
                    .orElseThrow(() -> new AppException(404, "chưa có chữ OCR của trang " + trang));
        } catch (IOException e) {
            throw new AppException(500, "đọc chữ OCR lỗi: " + e.getMessage());
        }
    }

    private Document find(String taiLieuId, int trang) {
        Document doc = repository.findById(taiLieuId)
                .orElseThrow(() -> new AppException(404, "không có tài liệu " + taiLieuId));
        if (trang < 1 || trang > doc.getTongSoTrang()) {
            throw new AppException(404, "trang " + trang + " ngoài 1.." + doc.getTongSoTrang());
        }
        return doc;
    }
}
