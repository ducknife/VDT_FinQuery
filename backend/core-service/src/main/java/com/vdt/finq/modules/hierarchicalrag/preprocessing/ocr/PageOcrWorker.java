package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.vdt.finq.common.properties.OCRProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// does one page: render -> call the model -> store, retrying down the dpi ladder
@Slf4j
@Component
@RequiredArgsConstructor
public class PageOcrWorker {

    // wait a little between attempts so a brief network dip can recover
    private static final long RETRY_PAUSE_MS = 2_000;

    private final OCRProperties properties;
    private final VisionOcrClient visionOcrClient;
    private final PageStore pageStore;

    public PageResult process(PdfRenderer renderer, String relativePath, int pageIndex) {
        String lastError = "unknown";

        for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
            int dpi = dpiForAttempt(attempt);
            try {
                byte[] jpeg = renderer.render(pageIndex, dpi, (float) properties.getJpegQuality());
                String markdown = visionOcrClient.ocr(jpeg);

                // page numbers on disk start at 1, pageIndex starts at 0
                pageStore.write(relativePath, pageIndex + 1, markdown);
                log.info("page {} ok on attempt {} at {} dpi", pageIndex + 1, attempt, dpi);
                return PageResult.ok(pageIndex, attempt);

            } catch (OcrTimeoutException e) {
                // most failures are the network, so the next attempt sends a smaller image
                lastError = e.getMessage();
                log.warn("page {} attempt {}/{} at {} dpi timed out: {}",
                        pageIndex + 1, attempt, properties.getMaxAttempts(), dpi, lastError);
                pause();

            } catch (IOException e) {
                lastError = e.getMessage();
                log.warn("page {} attempt {}/{} at {} dpi failed: {}",
                        pageIndex + 1, attempt, properties.getMaxAttempts(), dpi, lastError);
                pause();
            }
        }

        // out of attempts, leave a tombstone so the page is never lost silently
        log.error("page {} gave up after {} attempts: {}",
                pageIndex + 1, properties.getMaxAttempts(), lastError);
        try {
            pageStore.markFailed(relativePath, pageIndex + 1, lastError);
        } catch (IOException e) {
            log.error("could not write the .loi file for page {}", pageIndex + 1, e);
        }
        return PageResult.failed(pageIndex, properties.getMaxAttempts(), lastError);
    }

    // ladder is [0, 0, 130, 130, 110, 110], a 0 means use the default dpi
    private int dpiForAttempt(int attempt) {
        var ladder = properties.getDpiLadder();
        if (ladder == null || attempt > ladder.size()) {
            return properties.getDpi();
        }
        int dpi = ladder.get(attempt - 1);
        return dpi > 0 ? dpi : properties.getDpi();
    }

    private void pause() {
        try {
            Thread.sleep(RETRY_PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
