package com.vdt.finq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vdt.finq.common.properties.QdrantProperties;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

// một QdrantClient dùng chung cho cả ứng dụng, đóng kết nối khi tắt
@Configuration
public class QdrantConfig {

    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient(QdrantProperties properties) {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient
                .newBuilder(properties.getHost(), properties.getGrpcPort(), properties.isUseTls())
                .withTimeout(properties.getTimeout());
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder.withApiKey(properties.getApiKey());
        }
        return new QdrantClient(builder.build());
    }
}
