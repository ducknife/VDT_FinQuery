package com.vdt.finq.modules.rag.retrieval.rerank;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.properties.RerankProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

// gọi reranker DeepInfra; chấm từng cặp (câu hỏi, chunk) độc lập nên chia lô gửi song song không đổi điểm
@Slf4j
@Component
public class RerankClient {

    private final RerankProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // điểm của cả danh sách (đúng thứ tự gửi lên) và chi phí thật của các lần gọi
    @Getter
    @AllArgsConstructor
    public static class Scores {
        private final double[] scores;
        private final long inputTokens;
        private final double cost;
    }

    public RerankClient(RerankProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    }

    public Scores score(String model, String query, List<String> documents) throws IOException, InterruptedException {
        int batchSize = Math.max(1, properties.getBatchSize());
        List<List<String>> batches = new ArrayList<>();
        for (int start = 0; start < documents.size(); start += batchSize) {
            batches.add(documents.subList(start, Math.min(documents.size(), start + batchSize)));
        }
        double[] all = new double[documents.size()];
        long tokens = 0;
        double cost = 0;
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Scores>> futures = new ArrayList<>();
            for (List<String> batch : batches) {
                futures.add(pool.submit(() -> callWithRetry(model, query, batch)));
            }
            int offset = 0;
            for (int i = 0; i < futures.size(); i++) {
                Scores part = futures.get(i).get();
                System.arraycopy(part.getScores(), 0, all, offset, part.getScores().length);
                offset += part.getScores().length;
                tokens += part.getInputTokens();
                cost += part.getCost();
            }
        } catch (ExecutionException e) {
            throw e.getCause() instanceof IOException io ? io : new IOException(e.getCause());
        }
        return new Scores(all, tokens, cost);
    }

    // một lô; lỗi mạng, 429, 5xx thì thử lại, nghỉ tăng dần
    Scores callWithRetry(String model, String query, List<String> batch) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of("queries", List.of(query), "documents", batch));
        IOException lastError = null;
        for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
            try {
                return send(model, body, batch.size());
            } catch (RetryableException e) {
                lastError = e;
                log.warn("rerank {} lỗi lần {}/{}: {}", model, attempt, properties.getMaxAttempts(), e.getMessage());
                Thread.sleep(1500L * attempt);
            }
        }
        throw new IOException("rerank thất bại sau " + properties.getMaxAttempts() + " lần thử", lastError);
    }

    private Scores send(String model, String body, int expected) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getApiUrl() + "/" + model))
                .timeout(properties.getRequestTimeout())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RetryableException("lỗi mạng: " + e.getMessage(), e);
        }
        int status = response.statusCode();
        if (status == 429 || status >= 500) {
            throw new RetryableException("HTTP " + status + ": " + shorten(response.body()), null);
        }
        if (status != 200) {
            throw new IOException("HTTP " + status + ": " + shorten(response.body()));
        }
        return parse(response.body(), expected);
    }

    // tách riêng để test đọc response mẫu, không cần gọi API
    Scores parse(String json, int expected) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode scores = root.path("scores");
        // lệch một vị trí là gán nhầm điểm cho cả danh sách: báo lỗi ngay
        if (!scores.isArray() || scores.size() != expected) {
            throw new IOException("reranker trả " + scores.size() + " điểm, cần " + expected);
        }
        double[] values = new double[expected];
        for (int i = 0; i < expected; i++) {
            values[i] = scores.get(i).asDouble();
        }
        return new Scores(values, root.path("input_tokens").asLong(0),
                root.path("inference_status").path("cost").asDouble(0));
    }

    private static String shorten(String text) {
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }

    private static class RetryableException extends IOException {
        RetryableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
