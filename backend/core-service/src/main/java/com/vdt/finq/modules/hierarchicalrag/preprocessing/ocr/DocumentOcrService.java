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

// runs one whole document: works out which pages are missing and ocr them in parallel
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

            // skip pages that already have a .md, this is what makes a rerun cheap
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

            // the pool is shared by the whole application so ten uploads at once
            // still use only `concurrency` connections
            List<Future<PageResult>> futures = new ArrayList<>(todo.size());
            for (int pageIndex : todo) {
                futures.add(ocrPool.submit(
                        () -> worker.process(renderer, relativePath, pageIndex)));
            }

            int succeeded = 0;
            int failed = 0;
            for (Future<PageResult> future : futures) {
                try {
                    if (future.get().success()) {
                        succeeded++;
                    } else {
                        failed++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted while waiting for pages", e);
                } catch (ExecutionException e) {
                    // one broken page must not kill the whole document
                    log.error("page task threw", e.getCause());
                    failed++;
                }
            }

            log.info("{}: {} succeeded, {} failed", pdfPath.getFileName(), succeeded, failed);
            // caller sets documents.ngay_ocr only when result.complete() is true,
            // otherwise the failed pages would never be retried
            return new DocumentOcrResult(totalPages, skipped, succeeded, failed);
        }
    }
}
