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

// hạ tầng OCR dùng chung toàn ứng dụng
@Configuration
@RequiredArgsConstructor
public class OcrBeansConfig {

    private final OCRProperties ocrProperties;

    // một client cho cả ứng dụng, không tạo lại pool mỗi lần gọi
    @Bean
    public HttpClient ocrHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                // không đặt hạn giờ cả request; VisionOcrClient tự canh chunk đầu và khoảng lặng
                .build();
    }

    // một luồng duy nhất canh mọi lượt gọi đang chạy xem có bị im lặng không
    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService ocrWatchdog() {
        return Executors.newSingleThreadScheduledExecutor(named("ocr-watchdog"));
    }

    // số trang xử lý cùng lúc; nút thắt là mạng, không phải CPU
    @Bean(destroyMethod = "shutdown")
    public ExecutorService ocrPool() {
        return Executors.newFixedThreadPool(ocrProperties.getConcurrency(), named("ocr-worker"));
    }

    // đặt tên cho luồng để log và thread dump dễ đọc
    private static ThreadFactory named(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
