package com.vdt.finq.modules.hierarchicalrag.generation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.properties.GenerationProperties;
import com.vdt.finq.common.properties.LlmProperties;
import com.vdt.finq.modules.agent.ModelOptions;
import com.vdt.finq.modules.agent.evidence.Evidence;
import com.vdt.finq.modules.agent.prompts.SystemPrompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// sinh câu trả lời từ bằng chứng (paper Lee & Hong): vượt 64k token thì chia 2 nửa theo thứ hạng
// rồi hợp nhất; sau đó kiểm trích dẫn và con số bằng code
@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerGenerator {

    // ước lượng token từ số ký tự (tiếng Việt qua tokenizer Qwen khoảng 3 ký tự/token, chọn mức dè dặt)
    private static final double CHARS_PER_TOKEN = 3.0;

    private final ChatModel chatModel;
    private final LlmProperties llmProperties;
    private final GenerationProperties properties;
    private final ObjectMapper objectMapper;

    public AnswerResult generate(String question, String normalizedQuestion, List<Evidence> evidence)
            throws IOException {
        long start = System.nanoTime();
        int estimated = (int) ((SystemPrompt.ANSWER_SYSTEM_PROMPT.length() + question.length()
                + evidence.stream().mapToInt(e -> e.getContent().length() + 80).sum()) / CHARS_PER_TOKEN);
        if (estimated <= properties.getContextLimitTokens()) {
            Draft d = draft(question, normalizedQuestion, evidence);
            return finish(d, evidence, "single", List.of(d), start);
        }
        // vượt ngưỡng: số liệu DB đi cùng cả hai nửa, chunk chia theo thứ hạng
        List<Evidence> figures = evidence.stream().filter(e -> e.getKind() == Evidence.Kind.FIGURE).toList();
        List<Evidence> chunks = evidence.stream().filter(e -> e.getKind() == Evidence.Kind.CHUNK).toList();
        int half = (chunks.size() + 1) / 2;
        List<Evidence> first = new ArrayList<>(figures);
        first.addAll(chunks.subList(0, half));
        List<Evidence> second = new ArrayList<>(figures);
        second.addAll(chunks.subList(half, chunks.size()));
        log.info("ngữ cảnh ~{} token > {}: chia 2 nửa ({} / {} chunk)", estimated, properties.getContextLimitTokens(),
                half, chunks.size() - half);
        Draft a = draft(question, normalizedQuestion, first);
        Draft b = draft(question, normalizedQuestion, second);
        return finish(fuse(a, b), evidence, "split", List.of(a, b), start);
    }

    // một lần sinh: trả JSON đã đọc + số đo
    private Draft draft(String question, String normalizedQuestion, List<Evidence> evidence) throws IOException {
        Prompt prompt = new Prompt(List.of(new SystemMessage(SystemPrompt.ANSWER_SYSTEM_PROMPT),
                new UserMessage(userMessage(question, normalizedQuestion, evidence))), ModelOptions.answer(llmProperties));
        long t = System.nanoTime();
        ChatResponse response = chatModel.call(prompt);
        String content = response.getResult().getOutput().getText();
        log.info("sinh câu trả lời: {} bằng chứng, token vào {} ra {}, thinking {} ký tự, {} ms", evidence.size(),
                ModelOptions.promptTokens(response), ModelOptions.completionTokens(response),
                ModelOptions.reasoningChars(response), (System.nanoTime() - t) / 1_000_000);
        return new Draft(parseJson(objectMapper, content == null ? "" : content), ModelOptions.promptTokens(response),
                ModelOptions.completionTokens(response));
    }

    static String userMessage(String question, String normalizedQuestion, List<Evidence> evidence) {
        StringBuilder s = new StringBuilder("QUESTION (as typed by the user):\n").append(question).append("\n\n");
        if (normalizedQuestion != null && !normalizedQuestion.equals(question)) {
            s.append("NORMALIZED QUESTION:\n").append(normalizedQuestion).append("\n\n");
        }
        s.append("EVIDENCE:\n");
        if (evidence.isEmpty()) {
            s.append("(no evidence was found)\n");
        }
        for (Evidence e : evidence) {
            s.append('[').append(e.getId()).append("] (").append(e.source()).append(")\n")
                    .append(e.getContent().strip()).append("\n\n");
        }
        return s.toString();
    }

    // model đôi khi bọc JSON trong ```json ... ```: lấy từ { đầu tiên đến } cuối cùng
    static JsonNode parseJson(ObjectMapper json, String content) throws IOException {
        int from = content.indexOf('{');
        int to = content.lastIndexOf('}');
        if (from < 0 || to <= from) {
            throw new IOException("bước trả lời không trả JSON: " + shorten(content));
        }
        return json.readTree(content.substring(from, to + 1));
    }

    // hợp nhất 2 nửa: rõ ràng thắng không rõ; cả hai rõ thì bên nhiều trích dẫn hơn, bằng nhau lấy nửa đầu
    private static Draft fuse(Draft a, Draft b) {
        int ra = rank(a.parsed.path("status").asText());
        int rb = rank(b.parsed.path("status").asText());
        if (ra != rb) {
            return ra > rb ? a : b;
        }
        return b.parsed.path("citations").size() > a.parsed.path("citations").size() ? b : a;
    }

    private static int rank(String status) {
        return switch (status) {
            case "answered" -> 3;
            case "partial" -> 2;
            case "out_of_scope" -> 1;
            default -> 0;
        };
    }

    private AnswerResult finish(Draft chosen, List<Evidence> evidence, String mode, List<Draft> calls, long start) {
        JsonNode p = chosen.parsed;
        String answer = p.path("answer").asText("");
        List<Citation> citations = new ArrayList<>();
        for (JsonNode c : p.path("citations")) {
            String id = c.path("id").asText("");
            String quote = c.path("quote").asText("");
            Evidence e = evidence.stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
            citations.add(e == null
                    ? new Citation(id, null, null, 0, quote, false)
                    : new Citation(id, e.getTaiLieuId(), e.getTenHienThi(), e.getTrang(), quote,
                            CitationVerifier.quoteFound(quote, e.getContent())));
        }
        List<String> cited = CitationVerifier.citedIds(answer);
        List<String> known = evidence.stream().map(Evidence::getId).toList();
        List<String> unknown = cited.stream().filter(id -> !known.contains(id)).toList();
        // số trong câu trả lời phải có trong các bằng chứng được trích
        List<String> citedSources = evidence.stream().filter(e -> cited.contains(e.getId())).map(Evidence::getContent)
                .toList();
        List<String> badNumbers = CitationVerifier.unverifiedNumbers(answer, citedSources);
        int promptTokens = calls.stream().mapToInt(Draft::promptTokens).sum();
        int completionTokens = calls.stream().mapToInt(Draft::completionTokens).sum();
        return new AnswerResult(p.path("status").asText("not_found"), answer, citations, p.path("missing").asText(""),
                badNumbers, unknown, mode, promptTokens, completionTokens,
                llmProperties.cost(promptTokens, completionTokens), (System.nanoTime() - start) / 1_000_000);
    }

    private static String shorten(String s) {
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    private record Draft(JsonNode parsed, int promptTokens, int completionTokens) {
    }
}
