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

// hạ tầng dùng chung cho luồng OCR, cả ba bean đều dùng chung cho toàn ứng dụng.
// nếu mỗi tài liệu tự tạo riêng thì mười lượt tải lên sẽ mở sáu mươi kết nối
@Configuration
@RequiredArgsConstructor
public class OcrBeansConfig {

    private final OCRProperties ocrProperties;

    // một client cho cả ứng dụng, tạo mới mỗi lần gọi thì phải dựng lại
    // connection pool và thread pool 6690 lần trong một lượt chạy
    @Bean
    public HttpClient ocrHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                // không đặt hạn giờ cho cả request ở đây, VisionOcrClient tự canh
                // hai mốc: chờ chunk đầu tiên, rồi khoảng lặng giữa các chunk
                .build();
    }

    // một luồng duy nhất canh mọi lượt gọi đang chạy xem có bị im lặng không
    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService ocrWatchdog() {
        return Executors.newSingleThreadScheduledExecutor(named("ocr-watchdog"));
    }

    // giới hạn thật của luồng: bao nhiêu trang được xử lý cùng lúc.
    // phải giới hạn vì nút thắt là mạng, không phải cpu
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
