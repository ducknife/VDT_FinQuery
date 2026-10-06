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

// shared machinery for the ocr pipeline, all three beans are application wide.
// per document instances would mean ten uploads opening sixty connections
@Configuration
@RequiredArgsConstructor
public class OcrBeansConfig {

    private final OCRProperties ocrProperties;

    // one client for the whole app, a new one per call would rebuild its
    // connection pool and thread pool 6690 times in a run
    @Bean
    public HttpClient ocrHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                // no overall request timeout here, VisionOcrClient does its own
                // two part timing: first chunk, then silence between chunks
                .build();
    }

    // single thread watching every in flight call for silence
    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService ocrWatchdog() {
        return Executors.newSingleThreadScheduledExecutor(named("ocr-watchdog"));
    }

    // the real limit of the pipeline: how many pages may be in flight.
    // bounded because the bottleneck is the network, not the cpu
    @Bean(destroyMethod = "shutdown")
    public ExecutorService ocrPool() {
        return Executors.newFixedThreadPool(ocrProperties.getConcurrency(), named("ocr-worker"));
    }

    // names the threads so logs and thread dumps are readable
    private static ThreadFactory named(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
