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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends one page image to the vision model and returns the Markdown it reads
 * back.
 *
 * The request is streamed, and the timeout is split in two. That split is the
 * whole
 * point of this class - see {@link #ocr(byte[])}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisionOcrClient {

    /**
     * Exactly the prompt that produced the 6,690 verified pages. Do not reword it
     * without re-checking the output against the existing .md files - small changes
     * to the wording change how tables come out.
     */
    private static final String PROMPT = String.join("\n",
            "Extract the entire content of this page into Markdown format.",
            "- Every table must be formatted as a Markdown table, with each column separated correctly.",
            "- Retain all index codes, note numbers, and the EXACT full digits of every single number.",
            "- Keep parenthesized formats for negative numbers exactly as they are.",
            "- Leave empty cells blank; do not fill them with zeros.",
            "- Only return the page content. Do not include any explanations or introductory text.");

    private static final String SSE_PREFIX = "data:";
    private static final String SSE_DONE = "[DONE]";
    /**
     * Below this many characters the page is almost certainly a failed read, not a
     * short page.
     */
    private static final int MIN_USEFUL_LENGTH = 20; // below 20 almost is failed 

    private final OCRProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ScheduledExecutorService ocrWatchdog;

    /**
     * Reads one page image.
     *
     * <p>
     * Why the timeout is split in two: a single deadline for the whole page was
     * tried first and killed about 30% of pages, because the 75th percentile page
     * needs around 126 seconds. A long page is not a stuck page. So:
     *
     * <ul>
     * <li>first chunk: allowed up to {@code firstChunkTimeout} (uploading the image
     * plus the model looking at it)</li>
     * <li>after that: only the gap <em>between</em> chunks matters, capped at
     * {@code silenceTimeout}. Measured longest healthy gap was 4.4 seconds.</li>
     * <li>{@code totalTimeout} is a hard backstop in case both checks somehow
     * pass.</li>
     * </ul>
     *
     * While text keeps arriving the call keeps running, however long the page is.
     */
    public String ocr(byte[] jpeg) throws IOException {
        HttpRequest request = buildRequest(jpeg);

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while calling the OCR model", e);
        }

        if (response.statusCode() != 200) {
            throw new IOException("OCR model returned HTTP %d: %s"
                    .formatted(response.statusCode(), readErrorBody(response.body())));
        }

        String raw = readStream(response.body());
        String markdown = stripCodeFence(raw);

        if (markdown.isBlank() || markdown.length() < MIN_USEFUL_LENGTH) {
            // An empty answer is a failure, not an empty page. Treat it like a timeout
            // so the retry ladder gets a chance with a smaller image.
            throw new OcrTimeoutException("model returned almost nothing (%d chars)"
                    .formatted(markdown.length()));
        }
        return markdown;
    }

    // ---------------------------------------------------------------- request

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
                                Map.of("type", "text", "text", PROMPT),
                                Map.of("type", "image_url", "image_url",
                                        Map.of("url", "data:image/jpeg;base64," + base64))))));

        return HttpRequest.newBuilder()
                .uri(URI.create(properties.getApiUrl()))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                // No .timeout() on purpose - that would be a deadline for the whole
                // request, which is exactly the mistake this class avoids.
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
    }

    // ---------------------------------------------------------------- streaming

    /** Reads the SSE stream while a watchdog watches for silence. */
    private String readStream(InputStream body) throws IOException {
        long startNanos = System.nanoTime();
        AtomicLong lastChunkNanos = new AtomicLong(startNanos);
        AtomicBoolean gotFirstChunk = new AtomicBoolean(false);
        AtomicBoolean timedOut = new AtomicBoolean(false);

        ScheduledFuture<?> watch = startWatchdog(body, startNanos, lastChunkNanos,
                gotFirstChunk, timedOut);

        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String chunk = parseChunk(line);
                if (chunk == null) {
                    continue;
                }
                if (chunk.isEmpty()) {
                    break; // [DONE]
                }
                text.append(chunk);
                gotFirstChunk.set(true);
                lastChunkNanos.set(System.nanoTime());
            }
        } catch (IOException e) {
            // The watchdog closes the stream to break out of a blocked read(), so an
            // IOException here may be our own doing rather than a network fault.
            if (timedOut.get()) {
                throw new OcrTimeoutException(describeTimeout(gotFirstChunk.get(), startNanos));
            }
            throw e;
        } finally {
            watch.cancel(false);
        }

        if (timedOut.get()) {
            throw new OcrTimeoutException(describeTimeout(gotFirstChunk.get(), startNanos));
        }
        return text.toString();
    }

    /**
     * Closes the response body once the stream has gone quiet for too long.
     *
     * Closing from another thread is the only way to break an InputStream.read()
     * that is already blocked - Java has no read timeout for that.
     */
    private ScheduledFuture<?> startWatchdog(InputStream body, long startNanos,
            AtomicLong lastChunkNanos,
            AtomicBoolean gotFirstChunk,
            AtomicBoolean timedOut) {
        long firstChunkNanos = properties.getFirstChunkTimeout().toNanos();
        long silenceNanos = properties.getSilenceTimeout().toNanos();
        long totalNanos = properties.getTotalTimeout().toNanos();

        return ocrWatchdog.scheduleAtFixedRate(() -> {
            long now = System.nanoTime();
            long deadline = gotFirstChunk.get()
                    ? lastChunkNanos.get() + silenceNanos // silence between chunks
                    : startNanos + firstChunkNanos; // waiting for the first one
            boolean overall = now - startNanos > totalNanos;

            if (now > deadline || overall) {
                timedOut.set(true);
                try {
                    body.close(); // unblocks the reader
                } catch (IOException ignored) {
                    // Nothing useful to do here; the reader will surface the failure.
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Turns one SSE line into the text it carries.
     *
     * @return the text, {@code ""} when the stream is finished, or {@code null} for
     *         lines that carry nothing (blank lines, comments, role-only deltas)
     */
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
            JsonNode delta = objectMapper.readTree(payload)
                    .path("choices").path(0).path("delta").path("content");
            return delta.isTextual() ? delta.asText() : null;
        } catch (IOException e) {
            log.warn("skipping unreadable SSE line: {}", abbreviate(payload));
            return null;
        }
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Removes the ``` fence the model sometimes wraps the answer in. Left in place
     * it
     * would end up inside the .md file.
     */
    private static String stripCodeFence(String raw) {
        String text = raw.strip();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        if (firstNewline < 0) {
            return ""; // just a fence, no content
        }
        text = text.substring(firstNewline + 1); // drop ``` or ```markdown
        if (text.stripTrailing().endsWith("```")) {
            text = text.stripTrailing();
            text = text.substring(0, text.length() - 3);
        }
        return text.strip();
    }

    private String describeTimeout(boolean gotFirstChunk, long startNanos) {
        long seconds = (System.nanoTime() - startNanos) / 1_000_000_000L;
        return gotFirstChunk
                ? "stream went silent for more than %ds (total %ds)"
                        .formatted(properties.getSilenceTimeout().toSeconds(), seconds)
                : "no first chunk within %ds"
                        .formatted(properties.getFirstChunkTimeout().toSeconds());
    }

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
