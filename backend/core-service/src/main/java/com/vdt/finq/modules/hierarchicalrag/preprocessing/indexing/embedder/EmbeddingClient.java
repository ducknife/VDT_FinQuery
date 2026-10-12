package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.embedder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.properties.EmbeddingProperties;

import lombok.extern.slf4j.Slf4j;

// gọi API embeddings cho một lô văn bản, trả vector đúng thứ tự; xin dạng base64 (nhỏ hơn ~4 lần)
@Slf4j
@Component
public class EmbeddingClient {

    // 4 byte mỗi số thực float32
    private static final int FLOAT_BYTES = 4;

    private final EmbeddingProperties embeddingProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EmbeddingClient(EmbeddingProperties embeddingProperties, ObjectMapper objectMapper) {
        this.embeddingProperties = embeddingProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // embed một lô văn bản; lỗi mạng hoặc lỗi server thì thử lại tối đa maxAttempts lần
    public List<float[]> embed(List<String> texts) throws IOException, InterruptedException {
        if (texts.isEmpty()) {
            return List.of();
        }
        String body = objectMapper.writeValueAsString(Map.of(
                "model", embeddingProperties.getModel(),
                "input", texts,
                // dùng mã hoá based64 để giảm dung lượng lưu trữ cho qdrant
                "encoding_format", "base64"));

        IOException lastError = null;
        for (int attempt = 1; attempt <= embeddingProperties.getMaxAttempts(); attempt++) {
            try {
                return send(body, texts.size());
            } catch (RetryableException e) {
                lastError = e;
                log.warn("embedding lỗi lần {}/{}: {}", attempt, embeddingProperties.getMaxAttempts(), e.getMessage());
                // nghỉ tăng dần để mạng hoặc gateway kịp hồi lại
                Thread.sleep(2000L * attempt);
            }
        }
        throw new IOException("embedding thất bại sau " + embeddingProperties.getMaxAttempts() + " lần thử", lastError);
    }

    // một lần gọi API
    private List<float[]> send(String body, int expected) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(embeddingProperties.getApiUrl()))
                .timeout(embeddingProperties.getRequestTimeout())
                .header("Authorization", "Bearer " + embeddingProperties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        long start = System.nanoTime();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // lỗi mạng: đáng thử lại
            throw new RetryableException("lỗi mạng: " + e.getMessage(), e);
        }
        long millis = (System.nanoTime() - start) / 1_000_000;

        int status = response.statusCode();
        if (status == 429 || status >= 500) {
            throw new RetryableException("HTTP " + status + ": " + shorten(response.body()), null);
        }
        if (status != 200) {
            // lỗi 4xx khác (sai key, sai tên model...): thử lại cũng vô ích
            throw new IOException("HTTP " + status + ": " + shorten(response.body()));
        }

        List<float[]> vectors = parse(response.body(), expected);

        // chi phí của lần gọi, gateway LiteLLM trả trong header; ghi lại cho chương đánh giá
        String cost = response.headers().firstValue("x-litellm-response-cost").orElse("?");
        JsonNode usage = objectMapper.readTree(response.body()).path("usage");
        log.info("embedding {} văn bản: {} ms, {} token, chi phí {}",
                expected, millis, usage.path("prompt_tokens").asInt(-1), cost);
        return vectors;
    }

    // đọc data[] trong response, sắp theo index và giải mã base64 thành float[]
    private List<float[]> parse(String json, int expected) throws IOException {
        JsonNode data = objectMapper.readTree(json).path("data");
        if (!data.isArray() || data.size() != expected) {
            throw new IOException("gateway trả " + data.size() + " vector, cần " + expected);
        }
        float[][] ordered = new float[expected][];
        for (JsonNode item : data) {
            // gateway có thể trả không đúng thứ tự, nên đặt theo trường index
            int index = item.path("index").asInt();
            ordered[index] = decode(item.path("embedding").asText());
        }
        List<float[]> vectors = new ArrayList<>(expected);
        for (float[] vector : ordered) {
            if (vector == null || vector.length != embeddingProperties.getDimensions()) {
                throw new IOException("vector sai số chiều: " + (vector == null ? 0 : vector.length)
                        + ", cần " + embeddingProperties.getDimensions());
            }
            vectors.add(vector);
        }
        return vectors;
    }

    // base64 -> mảng float32 little-endian, đúng định dạng chuẩn OpenAI
    static float[] decode(String base64) {
        ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(base64)).order(ByteOrder.LITTLE_ENDIAN);
        float[] vector = new float[buffer.remaining() / FLOAT_BYTES];
        buffer.asFloatBuffer().get(vector);
        return vector;
    }

    private static String shorten(String text) {
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }

    // lỗi đáng thử lại: lỗi mạng, 429, 5xx
    private static class RetryableException extends IOException {
        RetryableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
