package com.vdt.finq.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngestionConfig {

    // một luồng xử lý lần lượt từng tài liệu; song song đã có ở mức trang (ocrPool)
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService ingestionExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ingestion");
            thread.setDaemon(true);
            return thread;
        });
    }
}
