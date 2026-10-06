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

// sends one page image to the vision model and returns markdown
@Slf4j
@Component
@RequiredArgsConstructor
public class VisionOcrClient {

    // no trailing space on purpose, the spec allows "data:" with or without one
    private static final String SSE_PREFIX = "data:";
    private static final String SSE_DONE = "[DONE]";

    // a floor, not a quality check. shortest real page in the corpus is 63 chars
    private static final int MIN_USEFUL_LENGTH = 20;

    private final OCRProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ScheduledExecutorService ocrWatchdog;

    // read one page image, throws OcrTimeoutException when the stream goes silent
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

        // an empty answer is a failure, not an empty page. throw the timeout type so
        // the retry ladder gets a chance with a smaller image
        if (markdown.isBlank() || markdown.length() < MIN_USEFUL_LENGTH) {
            throw new OcrTimeoutException("model returned almost nothing (%d chars)"
                    .formatted(markdown.length()));
        }
        return markdown;
    }

    // build the request body in the openai shape, which is what the gateway speaks
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
                // no .timeout() here, that would be a deadline for the whole call and
                // it is exactly the mistake this class avoids
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
    }

    // read the sse stream while the watchdog watches for silence
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
            // the watchdog closes the stream to break a blocked read, so this may be
            // our own doing rather than a network fault
            if (timedOut.get()) {
                throw new OcrTimeoutException(describeTimeout(gotFirstChunk.get(), startNanos));
            }
            throw e;
        } finally {
            watch.cancel(false);
        }

        // the stream may have ended cleanly just as the watchdog fired
        if (timedOut.get()) {
            throw new OcrTimeoutException(describeTimeout(gotFirstChunk.get(), startNanos));
        }
        return text.toString();
    }

    // closes the stream when it goes quiet, this is the only way to break a blocked read
    private ScheduledFuture<?> startWatchdog(InputStream body, long startNanos,
                                             AtomicLong lastChunkNanos,
                                             AtomicBoolean gotFirstChunk,
                                             AtomicBoolean timedOut) {
        long firstChunkNanos = properties.getFirstChunkTimeout().toNanos();
        long silenceNanos = properties.getSilenceTimeout().toNanos();
        long totalNanos = properties.getTotalTimeout().toNanos();

        return ocrWatchdog.scheduleAtFixedRate(() -> {
            long now = System.nanoTime();
            // before the first chunk we allow a long wait, after it only the gap matters
            long deadline = gotFirstChunk.get()
                    ? lastChunkNanos.get() + silenceNanos
                    : startNanos + firstChunkNanos;

            if (now > deadline || now - startNanos > totalNanos) {
                timedOut.set(true);
                try {
                    body.close();
                } catch (IOException ignored) {
                    // the reader will surface the failure
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    // one sse line -> its text, "" when the stream is done, null when it carries nothing
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
            return content.isTextual() ? content.asText() : null;
        } catch (IOException e) {
            // one bad line should not lose the whole page
            log.warn("skipping unreadable sse line: {}", abbreviate(payload));
            return null;
        }
    }

    // the model sometimes wraps the answer in ```, that would end up inside the .md
    private static String stripCodeFence(String raw) {
        String text = raw.strip();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        if (firstNewline < 0) {
            return "";
        }
        // drop the opening fence line, it may be ``` or ```markdown
        text = text.substring(firstNewline + 1).stripTrailing();
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.strip();
    }

    // says which of the two deadlines was hit
    private String describeTimeout(boolean gotFirstChunk, long startNanos) {
        long seconds = (System.nanoTime() - startNanos) / 1_000_000_000L;
        return gotFirstChunk
                ? "stream silent for more than %ds (total %ds)"
                        .formatted(properties.getSilenceTimeout().toSeconds(), seconds)
                : "no first chunk within %ds"
                        .formatted(properties.getFirstChunkTimeout().toSeconds());
    }

    // read an error body so the exception message can show it
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
