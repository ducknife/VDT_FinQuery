package com.vdt.finq.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vdt.finq.common.properties.OCRProperties;

import lombok.RequiredArgsConstructor;

/**
 * Shared machinery for the OCR pipeline.
 *
 * All three beans are application-wide on purpose. Creating them per document would
 * mean ten uploads at once opening sixty connections, which the network cannot take.
 */
@Configuration
@RequiredArgsConstructor
public class OcrBeansConfig {

    private final OCRProperties ocrProperties;

    /**
     * One client for the whole application. A new HttpClient per call would rebuild
     * the connection pool and its thread pool every time - 6,690 times over a run.
     */
    @Bean
    public HttpClient ocrHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                // No overall request timeout here: VisionOcrClient does its own
                // two-part timing (first chunk, then silence between chunks).
                .build();
    }

    /** Single thread that watches every in-flight OCR call for silence. */
    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService ocrWatchdog() {
        return Executors.newSingleThreadScheduledExecutor(named("ocr-watchdog"));
    }

    /**
     * The real concurrency limit of the pipeline: how many pages may be in flight.
     * Bounded because the bottleneck is the network, not the CPU.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService ocrPool() {
        return Executors.newFixedThreadPool(ocrProperties.getConcurrency(), named("ocr-worker"));
    }

    /** Names the threads so logs and thread dumps are readable. */
    private static ThreadFactory named(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
