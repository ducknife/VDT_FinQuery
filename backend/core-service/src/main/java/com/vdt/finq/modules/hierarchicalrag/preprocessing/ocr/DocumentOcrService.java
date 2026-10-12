package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// OCR trọn một tài liệu: tìm các trang còn thiếu rồi OCR song song
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentOcrService {

    private final PageStore pageStore;
    private final PageOcrWorker worker;
    private final ExecutorService ocrPool;

    public DocumentOcrResult ocrDocument(Path pdfPath, String relativePath) throws IOException {
        try (PdfRenderer renderer = PdfRenderer.open(pdfPath)) {
            int totalPages = renderer.pageCount();

            // bỏ qua trang đã có .md, nhờ vậy chạy lại rất rẻ
            List<Integer> todo = new ArrayList<>();
            for (int i = 0; i < totalPages; i++) {
                if (!pageStore.exists(relativePath, i + 1)) {
                    todo.add(i);
                }
            }
            int skipped = totalPages - todo.size();
            log.info("{}: {} pages, {} already done, {} to do",
                    pdfPath.getFileName(), totalPages, skipped, todo.size());

            if (todo.isEmpty()) {
                return new DocumentOcrResult(totalPages, skipped, 0, 0);
            }

            // pool dùng chung: nhiều lượt tải lên vẫn chỉ dùng `concurrency` kết nối
            List<Future<PageResult>> futures = new ArrayList<>(todo.size());
            for (int pageIndex : todo) {
                futures.add(ocrPool.submit(
                        () -> worker.process(renderer, relativePath, pageIndex)));
            }

            int succeeded = 0;
            int failed = 0;
            for (Future<PageResult> future : futures) {
                try {
                    if (future.get().isSuccess()) {
                        succeeded++;
                    } else {
                        failed++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted while waiting for pages", e);
                } catch (ExecutionException e) {
                    // một trang lỗi không được làm hỏng cả tài liệu
                    log.error("page task threw", e.getCause());
                    failed++;
                }
            }

            log.info("{}: {} succeeded, {} failed", pdfPath.getFileName(), succeeded, failed);
            // chỉ ghi ngay_ocr khi result.isComplete(), để trang lỗi còn được thử lại
            return new DocumentOcrResult(totalPages, skipped, succeeded, failed);
        }
    }
}
