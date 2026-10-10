package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.properties.OCRProperties;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.prompts.OcrSystemPrompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// gửi ảnh một trang tới LLM và nhận về markdown
@Slf4j
@Component
@RequiredArgsConstructor
public class VisionOcrClient {

    // cố ý không có dấu cách ở cuối, chuẩn SSE cho phép "data:" có hoặc không có dấu cách
    private static final String SSE_PREFIX = "data:";
    private static final String SSE_DONE = "[DONE]";

    // chỉ là mức sàn, không phải kiểm tra chất lượng. trang thật ngắn nhất trong kho dài 63 ký tự
    private static final int MIN_USEFUL_LENGTH = 20;

    private final OCRProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ScheduledExecutorService ocrWatchdog;

    // đọc ảnh một trang, ném OcrTimeoutException khi stream im lặng quá lâu
    public String ocr(byte[] jpeg) throws IOException {
        HttpRequest request = buildRequest(jpeg);

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while calling the ocr model", e);
        }

        if (response.statusCode() != 200) {
            throw new IOException("ocr model returned http %d: %s"
                    .formatted(response.statusCode(), readErrorBody(response.body())));
        }

        String markdown = stripCodeFence(readStream(response.body()));

        // câu trả lời rỗng là lỗi, không phải trang trống. ném lỗi hết giờ để
        // thang thử lại có cơ hội chạy với ảnh nhỏ hơn
        if (markdown.isBlank() || markdown.length() < MIN_USEFUL_LENGTH) {
            throw new OcrTimeoutException("model returned almost nothing (%d chars)"
                    .formatted(markdown.length()));
        }
        return markdown;
    }

    // dựng body của request theo định dạng OpenAI, định dạng mà gateway hiểu
    private HttpRequest buildRequest(byte[] jpeg) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(jpeg);

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "max_tokens", properties.getMaxTokens(),
                "temperature", 0,
                "stream", true,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", OcrSystemPrompt.OCR_SYSTEM_PROMPT),
                                Map.of("type", "image_url", "image_url",
                                        Map.of("url", "data:image/jpeg;base64," + base64))))));

        return HttpRequest.newBuilder()
                .uri(URI.create(properties.getApiUrl()))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                // không đặt .timeout() ở đây, vì đó là hạn giờ cho cả lượt gọi,
                // đúng sai lầm mà class này tránh
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
    }

    // đọc stream SSE trong khi watchdog canh khoảng lặng
    private String readStream(InputStream body) throws IOException {
        long startNanos = System.nanoTime();
        AtomicLong lastChunkNanos = new AtomicLong(startNanos);
        AtomicBoolean gotFirstChunk = new AtomicBoolean(false);
        AtomicBoolean timedOut = new AtomicBoolean(false);

        ScheduledFuture<?> watch =
                startWatchdog(body, startNanos, lastChunkNanos, gotFirstChunk, timedOut);

        StringBuilder text = new StringBuilder();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String chunk = parseChunk(line);
                // bỏ qua lỗi hoặc là content rỗng do deepinfra khai báo content: "" dễ bị end lỗi
                if (chunk == null) {
                    continue;
                }
                if (chunk.isEmpty()) {
                    break;
                }
                text.append(chunk);
                gotFirstChunk.set(true);
                lastChunkNanos.set(System.nanoTime());
            }
        } catch (IOException e) {
            // watchdog đóng stream để phá lệnh đọc đang bị chặn, nên lỗi này có thể
            // do chính mình gây ra chứ không phải lỗi mạng
            if (timedOut.get()) {
                throw new OcrTimeoutException(describeTimeout(gotFirstChunk.get(), startNanos));
            }
            throw e;
        } finally {
            watch.cancel(false);
        }

        // stream có thể vừa kết thúc bình thường đúng lúc watchdog kích hoạt
        if (timedOut.get()) {
            throw new OcrTimeoutException(describeTimeout(gotFirstChunk.get(), startNanos));
        }
        return text.toString();
    }

    // đóng stream khi nó im lặng, đây là cách duy nhất để phá lệnh đọc đang bị chặn
    private ScheduledFuture<?> startWatchdog(InputStream body, long startNanos,
                                             AtomicLong lastChunkNanos,
                                             AtomicBoolean gotFirstChunk,
                                             AtomicBoolean timedOut) {
        long firstChunkNanos = properties.getFirstChunkTimeout().toNanos();
        long silenceNanos = properties.getSilenceTimeout().toNanos();
        long totalNanos = properties.getTotalTimeout().toNanos();

        return ocrWatchdog.scheduleAtFixedRate(() -> {
            long now = System.nanoTime();
            // trước chunk đầu tiên thì cho chờ lâu, sau đó chỉ xét khoảng lặng giữa các chunk
            long deadline = gotFirstChunk.get()
                    ? lastChunkNanos.get() + silenceNanos
                    : startNanos + firstChunkNanos;

            if (now > deadline || now - startNanos > totalNanos) {
                timedOut.set(true);
                try {
                    body.close();
                } catch (IOException ignored) {
                    // luồng đọc sẽ báo lỗi ra ngoài
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    // một dòng SSE -> phần chữ của nó; "" khi stream kết thúc, null khi dòng không chứa gì
    private String parseChunk(String line) {
        if (line.isBlank() || !line.startsWith(SSE_PREFIX)) {
            return null;
        }
        String payload = line.substring(SSE_PREFIX.length()).trim();
        if (payload.isEmpty()) {
            return null;
        }
        if (SSE_DONE.equals(payload)) {
            return "";
        }
        try {
            JsonNode content = objectMapper.readTree(payload)
                    .path("choices").path(0).path("delta").path("content");
            // chunk đầu của DeepInfra chỉ khai báo role và có content = "": phải bỏ qua,
            // vì "" ở đây là tín hiệu hết stream
            return content.isTextual() && !content.asText().isEmpty() ? content.asText() : null;
        } catch (IOException e) {
            // một dòng hỏng không được làm mất cả trang
            log.warn("skipping unreadable sse line: {}", abbreviate(payload));
            return null;
        }
    }

    // model đôi khi bọc câu trả lời trong ```, không bỏ đi thì sẽ lọt vào file .md
    private static String stripCodeFence(String raw) {
        String text = raw.strip();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        if (firstNewline < 0) {
            return "";
        }
        // bỏ dòng hàng rào mở đầu, có thể là ``` hoặc ```markdown
        text = text.substring(firstNewline + 1).stripTrailing();
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.strip();
    }

    // cho biết đã chạm hạn giờ nào trong hai hạn
    private String describeTimeout(boolean gotFirstChunk, long startNanos) {
        long seconds = (System.nanoTime() - startNanos) / 1_000_000_000L;
        return gotFirstChunk
                ? "stream silent for more than %ds (total %ds)"
                        .formatted(properties.getSilenceTimeout().toSeconds(), seconds)
                : "no first chunk within %ds"
                        .formatted(properties.getFirstChunkTimeout().toSeconds());
    }

    // đọc body lỗi để thông báo của exception hiển thị được
    private String readErrorBody(InputStream body) {
        try (InputStream in = body) {
            return abbreviate(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return "(error body unreadable)";
        }
    }

    private static String abbreviate(String text) {
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }
}
