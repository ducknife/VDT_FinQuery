package com.vdt.finq.modules.hierarchicalrag.generation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.modules.agent.evidence.Evidence;
import com.vdt.finq.modules.agent.evidence.EvidenceStore;
import com.vdt.finq.modules.agent.prompts.SystemPrompt;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievalOptions;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievalService;

// đo tách bước sinh câu trả lời (chờ token đầu, thinking, viết, token/giây) bằng stream API trực tiếp,
// cùng đầu vào V02 cho mọi biến thể; ghi ra data/eval/ket-qua/do-tre-llm_*.md:
//     ./mvnw test "-Dtest=LlmLatencyProbeTest" "-Dllmprobe=true" ["-Dllmprobe.variants=..." "-Dllmprobe.reps=2"]
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "llmprobe", matches = "true")
class LlmLatencyProbeTest {

    private static final Path OUT_DIR = Path.of("../../data/eval/ket-qua");
    private static final String QUESTION = "Nguyên nhân lãi ròng hợp nhất quý 3/2025 tăng so với cùng kỳ là gì?";
    // đúng 3 câu tìm agent đã gọi ở lần chạy 20:01 (agent-thu_20261012-200117.md)
    private static final List<String> AGENT_QUERIES = List.of(
            "nguyên nhân lãi ròng hợp nhất quý 3 năm 2025 tăng so với cùng kỳ",
            "giải trình chênh lệch kết quả kinh doanh quý III/2025 so với cùng kỳ nguyên nhân lợi nhuận sau thuế tăng",
            "Công văn giải trình 2025 ngày 25/10/2025 trang 2-3 nguyên nhân lợi nhuận sau thuế quý III/2025 tăng");
    private static final String SONNET = "anthropic/claude-sonnet-5-5";

    private record Variant(String name, boolean allEvidence, String model, Boolean thinking) {
    }

    private record Sample(String variant, int rep, long firstByteMs, long firstReasoningMs, long firstContentMs,
            long totalMs, int promptTokens, int completionTokens, int reasoningTokens, int reasoningChars,
            int contentChars, String error) {
    }

    @Autowired
    private RetrievalService retrievalService;
    @Autowired
    private ObjectMapper json;
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    @Value("${llm.model}")
    private String model;

    @Test
    void measure() throws Exception {
        int reps = Integer.parseInt(System.getProperty("llmprobe.reps", "3"));
        List<Variant> all = List.of(
                new Variant("E20_THINK", false, model, true),
                new Variant("E20_NOTHINK", false, model, false),
                new Variant("EALL_THINK", true, model, true),
                new Variant("E20_SONNET", false, SONNET, null));
        String wanted = System.getProperty("llmprobe.variants", "");
        List<Variant> variants = wanted.isBlank() ? all
                : all.stream().filter(v -> List.of(wanted.split(",")).contains(v.name())).toList();

        // dựng bằng chứng MỘT lần, mọi biến thể dùng chung
        String normalized = QUESTION;
        EvidenceStore one = new EvidenceStore();
        one.addChunks(retrievalService.retrieve(AGENT_QUERIES.get(0), RetrievalOptions.DEFAULT).getChunks());
        EvidenceStore three = new EvidenceStore();
        for (String q : AGENT_QUERIES) {
            three.addChunks(retrievalService.retrieve(q, RetrievalOptions.DEFAULT).getChunks());
        }
        Map<Boolean, List<Evidence>> evidence = Map.of(false, one.all(), true, three.all());

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        Path rawDir = OUT_DIR.resolve("do-tre-llm_" + stamp + "_tho");
        Files.createDirectories(rawDir);
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        List<Sample> samples = new ArrayList<>();
        for (int rep = 1; rep <= reps; rep++) {
            for (Variant v : variants) {
                List<Evidence> ev = evidence.get(v.allEvidence());
                String user = AnswerGenerator.userMessage(QUESTION, normalized, ev);
                Sample s = call(http, v, rep, user, rawDir);
                samples.add(s);
                System.out.printf("[%s #%d] đầu=%d ms, thinking đầu=%d, chữ đầu=%d, tổng=%d ms, vào=%d ra=%d (nháp %d) %s%n",
                        v.name(), rep, s.firstByteMs(), s.firstReasoningMs(), s.firstContentMs(), s.totalMs(),
                        s.promptTokens(), s.completionTokens(), s.reasoningTokens(), s.error() == null ? "" : s.error());
            }
        }
        Path md = OUT_DIR.resolve("do-tre-llm_" + stamp + ".md");
        Files.writeString(md, report(samples, variants, evidence), StandardCharsets.UTF_8);
        System.out.println("đã ghi " + md.toAbsolutePath().normalize());
    }

    private Sample call(HttpClient http, Variant v, int rep, String user, Path rawDir) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", v.model());
        body.put("temperature", 0.0);
        body.put("max_tokens", 6000);
        body.put("stream", true);
        body.put("stream_options", Map.of("include_usage", true));
        body.put("messages", List.of(Map.of("role", "system", "content", SystemPrompt.ANSWER_SYSTEM_PROMPT),
                Map.of("role", "user", "content", user)));
        if (v.thinking() != null) {
            body.put("chat_template_kwargs", Map.of("enable_thinking", v.thinking()));
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofMinutes(5))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();

        long start = System.nanoTime();
        long firstByte = -1;
        long firstReasoning = -1;
        long firstContent = -1;
        StringBuilder reasoning = new StringBuilder();
        StringBuilder content = new StringBuilder();
        JsonNode usage = null;
        HttpResponse<java.io.InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            String err = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
            return new Sample(v.name(), rep, -1, -1, -1, ms(start), 0, 0, 0, 0, 0,
                    "HTTP " + resp.statusCode() + ": " + (err.length() > 200 ? err.substring(0, 200) : err));
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                if (firstByte < 0) {
                    firstByte = ms(start);
                }
                String data = line.substring(5).strip();
                if (data.equals("[DONE]")) {
                    break;
                }
                JsonNode n = json.readTree(data);
                if (n.hasNonNull("usage") && n.get("usage").has("prompt_tokens")) {
                    usage = n.get("usage");
                }
                JsonNode delta = n.path("choices").path(0).path("delta");
                // nhà cung cấp khác nhau đặt phần nháp ở reasoning_content hoặc reasoning
                String rc = delta.hasNonNull("reasoning_content") ? delta.get("reasoning_content").asText()
                        : delta.hasNonNull("reasoning") ? delta.get("reasoning").asText() : "";
                if (!rc.isEmpty()) {
                    if (firstReasoning < 0) {
                        firstReasoning = ms(start);
                    }
                    reasoning.append(rc);
                }
                String c = delta.path("content").asText("");
                if (!c.isEmpty()) {
                    if (firstContent < 0) {
                        firstContent = ms(start);
                    }
                    content.append(c);
                }
            }
        }
        long total = ms(start);
        Files.writeString(rawDir.resolve(v.name() + "_" + rep + ".txt"),
                "=== THINKING (" + reasoning.length() + " ký tự)\n" + reasoning + "\n\n=== CÂU TRẢ LỜI (" + content.length()
                        + " ký tự)\n" + content + "\n\n=== USAGE\n" + usage + "\n", StandardCharsets.UTF_8);
        int pt = usage == null ? 0 : usage.path("prompt_tokens").asInt();
        int ct = usage == null ? 0 : usage.path("completion_tokens").asInt();
        int rt = usage == null ? 0 : usage.path("completion_tokens_details").path("reasoning_tokens").asInt();
        return new Sample(v.name(), rep, firstByte, firstReasoning, firstContent, total, pt, ct, rt,
                reasoning.length(), content.length(), null);
    }

    private static long ms(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static String report(List<Sample> samples, List<Variant> variants, Map<Boolean, List<Evidence>> evidence) {
        StringBuilder s = new StringBuilder("# Đo độ trễ bước sinh câu trả lời — "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n\n");
        s.append("Câu hỏi V02: ").append(QUESTION).append("  \n");
        s.append("Bằng chứng: E20 = ").append(evidence.get(false).size()).append(" chunk (1 lần tìm), EALL = ")
                .append(evidence.get(true).size()).append(" chunk (3 lần tìm như agent). Gọi thẳng API, stream, temperature 0.\n\n");
        s.append("- **Chờ token đầu** = xếp hàng ở nhà cung cấp + đọc prompt (prefill)\n");
        s.append("- **Thinking** = từ token đầu đến chữ đầu của câu trả lời\n");
        s.append("- **Viết** = từ chữ đầu của câu trả lời đến hết\n\n");
        s.append("| Biến thể | Lượt | Chờ token đầu | Thinking | Viết | Tổng | Token vào | Token ra | Trong đó nháp | Ký tự nháp | Ký tự trả lời | Token ra/giây | Lỗi |\n");
        s.append("|---|---|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (Sample x : samples) {
            long ttft = x.firstReasoningMs() >= 0 ? x.firstReasoningMs() : x.firstContentMs();
            long think = x.firstReasoningMs() >= 0 && x.firstContentMs() >= 0 ? x.firstContentMs() - x.firstReasoningMs() : 0;
            long write = x.firstContentMs() >= 0 ? x.totalMs() - x.firstContentMs() : 0;
            long gen = x.totalMs() - Math.max(ttft, 0);
            String tps = gen > 0 && x.completionTokens() > 0 ? String.format("%.1f", x.completionTokens() * 1000.0 / gen) : "";
            s.append(String.format("| %s | %d | %.1f s | %.1f s | %.1f s | %.1f s | %d | %d | %d | %d | %d | %s | %s |%n",
                    x.variant(), x.rep(), ttft / 1000.0, think / 1000.0, write / 1000.0, x.totalMs() / 1000.0,
                    x.promptTokens(), x.completionTokens(), x.reasoningTokens(), x.reasoningChars(), x.contentChars(), tps,
                    x.error() == null ? "" : x.error().replace('|', '/')));
        }
        s.append("\n## Trung vị theo biến thể\n\n| Biến thể | Chờ token đầu | Tổng | Token ra |\n|---|---|---|---|\n");
        for (Variant v : variants) {
            List<Sample> xs = samples.stream().filter(x -> x.variant().equals(v.name()) && x.error() == null).toList();
            if (xs.isEmpty()) {
                continue;
            }
            s.append(String.format("| %s | %.1f s | %.1f s | %d |%n", v.name(),
                    median(xs.stream().mapToLong(x -> x.firstReasoningMs() >= 0 ? x.firstReasoningMs() : x.firstContentMs()).toArray()) / 1000.0,
                    median(xs.stream().mapToLong(Sample::totalMs).toArray()) / 1000.0,
                    (int) median(xs.stream().mapToLong(Sample::completionTokens).toArray())));
        }
        return s.toString();
    }

    private static double median(long[] xs) {
        long[] a = xs.clone();
        java.util.Arrays.sort(a);
        return a.length == 0 ? 0 : a.length % 2 == 1 ? a[a.length / 2] : (a[a.length / 2 - 1] + a[a.length / 2]) / 2.0;
    }
}
