package com.vdt.finq.modules.rag.preprocessing.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.vdt.finq.common.properties.OCRProperties;

// server giả trả stream SSE, không gọi API thật
class VisionOcrClientTest {

    private static final String PAGE = "# Bảng cân đối kế toán\n\n| Chỉ tiêu | Số cuối kỳ |\n|---|---|\n| Tiền | 1.234.567 |";

    private HttpServer server;
    private String sseBody;
    private ScheduledExecutorService watchdog;
    private VisionOcrClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/chat", exchange -> {
            byte[] bytes = sseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();

        OCRProperties properties = new OCRProperties();
        properties.setApiUrl("http://localhost:" + server.getAddress().getPort() + "/chat");
        properties.setApiKey("test");
        properties.setModel("test-model");
        properties.setMaxTokens(100L);
        properties.setFirstChunkTimeout(Duration.ofSeconds(10));
        properties.setSilenceTimeout(Duration.ofSeconds(10));
        properties.setTotalTimeout(Duration.ofSeconds(30));

        watchdog = Executors.newSingleThreadScheduledExecutor();
        client = new VisionOcrClient(properties, new ObjectMapper(), HttpClient.newHttpClient(), watchdog);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        watchdog.shutdownNow();
    }

    // DeepInfra: chunk đầu chỉ có role, content = "" -> không được hiểu nhầm là hết stream
    @Test
    void skipsEmptyRoleChunk() throws Exception {
        sseBody = chunk("") + chunk("```markdown\n") + chunk(PAGE.substring(0, 30))
                + chunk(PAGE.substring(30)) + chunk("\n```") + "data: [DONE]\n\n";

        assertThat(client.ocr(new byte[] {1})).isEqualTo(PAGE);
    }

    // content = null (kiểu gateway cũ) vẫn đọc được
    @Test
    void skipsNullContent() throws Exception {
        sseBody = "data: {\"choices\":[{\"delta\":{\"role\":\"assistant\",\"content\":null}}]}\n\n"
                + chunk(PAGE) + "data: [DONE]\n\n";

        assertThat(client.ocr(new byte[] {1})).isEqualTo(PAGE);
    }

    // model thật sự trả rỗng thì vẫn báo lỗi để thang thử lại chạy
    @Test
    void stillRejectsEmptyAnswer() throws Exception {
        sseBody = chunk("") + chunk("ok") + "data: [DONE]\n\n";

        assertThatThrownBy(() -> client.ocr(new byte[] {1}))
                .isInstanceOf(OcrTimeoutException.class)
                .hasMessageContaining("almost nothing (2 chars)");
    }

    // một dòng SSE theo đúng dạng DeepInfra gửi
    private static String chunk(String content) throws Exception {
        String json = new ObjectMapper().writeValueAsString(content);
        return "data: {\"object\":\"chat.completion.chunk\",\"choices\":[{\"index\":0,\"delta\":"
                + "{\"role\":\"assistant\",\"content\":" + json + ",\"reasoning_content\":null}}]}\n\n";
    }
}
