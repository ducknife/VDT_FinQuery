package com.vdt.finq.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngestionConfig {

    // một luồng duy nhất xử lý lần lượt từng tài liệu tải lên.
    // song song đã có ở bên trong (ocrPool OCR nhiều trang cùng lúc),
    // chạy nhiều tài liệu cùng lúc chỉ làm nghẽn API và tốn RAM
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService ingestionExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ingestion");
            thread.setDaemon(true);
            return thread;
        });
    }
}
