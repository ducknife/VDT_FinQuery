package com.vdt.finq.common.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// cấu hình reranker hai tầng, đọc từ khối rerank trong application.yml
@Configuration
@ConfigurationProperties(prefix = "rerank")
@Getter
@Setter
@NoArgsConstructor
public class RerankProperties {
    private String apiUrl;
    private String apiKey;
    private String tier1Model = "";
    private String tier2Model = "";
    private int batchSize = 40;
    private Duration requestTimeout = Duration.ofSeconds(60);
    private int maxAttempts = 3;
}
