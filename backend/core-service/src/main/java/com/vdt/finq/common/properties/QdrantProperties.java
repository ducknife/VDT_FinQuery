package com.vdt.finq.common.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// cấu hình kết nối Qdrant, đọc từ khối qdrant trong application.yml
@Configuration
@ConfigurationProperties(prefix = "qdrant")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QdrantProperties {

    private String host = "localhost";

    // Java dùng gRPC (6334); cổng 6333 là REST và dashboard
    private int grpcPort = 6334;

    private boolean useTls = false;

    // Qdrant chạy ở máy con không đặt key; để trống thì không gửi
    private String apiKey = "";

    // tên collection ghi kèm tên model, đổi model thì đổi collection
    private String collection = "finq_pages_qwen3e8b";

    // số point mỗi lần upsert
    private int upsertBatchSize = 64;

    private Duration timeout = Duration.ofSeconds(30);
}
