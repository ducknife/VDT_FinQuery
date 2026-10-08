package com.vdt.finq.common.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// cấu hình gọi model embedding, đọc từ khối embedding trong application.yml
@Configuration
@ConfigurationProperties(prefix = "embedding")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmbeddingProperties {

    // endpoint /v1/embeddings của gateway (chuẩn OpenAI)
    private String apiUrl;
    private String apiKey;

    // tên model phải viết đúng như gateway, ví dụ Qwen3-Embedding-4B (không có tiền tố Qwen/)
    private String model;

    // số chiều của vector, dùng để kiểm tra kết quả trả về
    private int dimensions = 2560;

    // số chunk mỗi lần gọi; đo trên gateway: 32 nhanh nhất (0,098 giây mỗi chunk)
    private int batchSize = 32;

    // hạn giờ cho một lần gọi
    private Duration requestTimeout = Duration.ofSeconds(120);

    // số lần thử khi lỗi mạng hoặc lỗi phía server
    private int maxAttempts = 3;
}
