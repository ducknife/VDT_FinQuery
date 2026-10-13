package com.vdt.finq.modules.rag.generation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
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
        return generate(question, normalizedQuestion, evidence, null);
    }

    // onDelta khác null: stream chữ của câu trả lời ra ngay khi sinh (chỉ khi không phải chia 2 nửa);
    // kiểm trích dẫn và con số vẫn chạy sau khi sinh xong như cũ
    public AnswerResult generate(String question, String normalizedQuestion, List<Evidence> evidence,
            Consumer<String> onDelta) throws IOException {
        return generate(question, normalizedQuestion, evidence, onDelta, "");
    }

    // context: lịch sử hội thoại và bài học từ feedback; chỉ để hiểu câu hỏi, không phải bằng chứng
    public AnswerResult generate(String question, String normalizedQuestion, List<Evidence> evidence,
            Consumer<String> onDelta, String context) throws IOException {
        long start = System.nanoTime();
        int estimated = (int) ((SystemPrompt.ANSWER_SYSTEM_PROMPT.length() + question.length()
                + evidence.stream().mapToInt(e -> e.getContent().length() + 80).sum()) / CHARS_PER_TOKEN);
        if (estimated <= properties.getContextLimitTokens()) {
            Draft d = onDelta == null ? draft(question, normalizedQuestion, evidence, context)
                    : streamDraft(question, normalizedQuestion, evidence, onDelta, context);
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
        Draft a = draft(question, normalizedQuestion, first, context);
        Draft b = draft(question, normalizedQuestion, second, context);
        return finish(fuse(a, b), evidence, "split", List.of(a, b), start);
    }

    // một lần sinh: trả JSON đã đọc + số đo
    private Draft draft(String question, String normalizedQuestion, List<Evidence> evidence, String context)
            throws IOException {
        Prompt prompt = new Prompt(List.of(new SystemMessage(SystemPrompt.ANSWER_SYSTEM_PROMPT),
                new UserMessage(userMessage(question, normalizedQuestion, evidence, context))), ModelOptions.answer(llmProperties));
        long t = System.nanoTime();
        ChatResponse response = chatModel.call(prompt);
        String content = response.getResult().getOutput().getText();
        log.info("sinh câu trả lời: {} bằng chứng, token vào {} ra {}, thinking {} ký tự, {} ms", evidence.size(),
                ModelOptions.promptTokens(response), ModelOptions.completionTokens(response),
                ModelOptions.reasoningChars(response), (System.nanoTime() - t) / 1_000_000);
        return new Draft(parseJson(objectMapper, content == null ? "" : content), ModelOptions.promptTokens(response),
                ModelOptions.completionTokens(response));
    }

    // như draft nhưng stream: đẩy chữ của trường "answer" ra onDelta khi model đang viết
    private Draft streamDraft(String question, String normalizedQuestion, List<Evidence> evidence,
            Consumer<String> onDelta, String context) throws IOException {
        Prompt prompt = new Prompt(List.of(new SystemMessage(SystemPrompt.ANSWER_SYSTEM_PROMPT),
                new UserMessage(userMessage(question, normalizedQuestion, evidence, context))),
                ModelOptions.answerStreaming(llmProperties));
        long t = System.nanoTime();
        AnswerFieldStreamer streamer = new AnswerFieldStreamer();
        int in = 0;
        int out = 0;
        for (ChatResponse r : chatModel.stream(prompt).toIterable()) {
            if (r.getResult() != null && r.getResult().getOutput() != null) {
                String delta = streamer.feed(r.getResult().getOutput().getText());
                if (!delta.isEmpty()) {
                    onDelta.accept(delta);
                }
            }
            // usage chỉ có ở mẩu cuối (stream_options.include_usage)
            in = Math.max(in, ModelOptions.promptTokens(r));
            out = Math.max(out, ModelOptions.completionTokens(r));
        }
        log.info("sinh câu trả lời (stream): {} bằng chứng, token vào {} ra {}, {} ms", evidence.size(), in, out,
                (System.nanoTime() - t) / 1_000_000);
        return new Draft(parseJson(objectMapper, streamer.raw()), in, out);
    }

    static String userMessage(String question, String normalizedQuestion, List<Evidence> evidence) {
        return userMessage(question, normalizedQuestion, evidence, "");
    }

    static String userMessage(String question, String normalizedQuestion, List<Evidence> evidence, String context) {
        StringBuilder s = new StringBuilder("QUESTION (as typed by the user):\n").append(question).append("\n\n");
        if (normalizedQuestion != null && !normalizedQuestion.equals(question)) {
            s.append("NORMALIZED QUESTION:\n").append(normalizedQuestion).append("\n\n");
        }
        if (context != null && !context.isBlank()) {
            s.append("CONTEXT (earlier conversation and lessons from feedback; use it only to understand the question, ")
                    .append("it is NOT evidence and must not be cited):\n").append(context.strip()).append("\n\n");
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

    // model đôi khi bọc JSON trong ```json ... ```, viết chữ trước/sau, hoặc bọc thừa ngoặc "{{ ... }}":
    // thử từng dấu { và đọc một object trọn vẹn, bỏ phần thừa phía sau; lấy object đầu tiên có "answer"
    static JsonNode parseJson(ObjectMapper json, String content) throws IOException {
        IOException first = null;
        for (int from = content.indexOf('{'); from >= 0; from = content.indexOf('{', from + 1)) {
            try (JsonParser p = json.getFactory().createParser(content.substring(from))) {
                JsonNode node = json.readTree(p);
                if (node != null && node.has("answer")) {
                    if (from > 0 && !content.substring(0, from).isBlank()) {
                        log.warn("bước trả lời có phần thừa trước JSON: {}", shorten(content.substring(0, from)));
                    }
                    return node;
                }
            } catch (IOException e) {
                if (first == null) {
                    first = e;
                }
            }
        }
        log.warn("bước trả lời không đọc được JSON, đầu: {} | cuối: {}", shorten(content),
                content.length() > 300 ? content.substring(content.length() - 300) : "");
        throw first != null ? first : new IOException("bước trả lời không trả JSON: " + shorten(content));
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
        List<EvidenceCalculator.Calculation> calculations = EvidenceCalculator.run(p.path("calculations"), evidence);
        String answer = EvidenceCalculator.fill(p.path("answer").asText(""), calculations);
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
        List<String> citedSources = new ArrayList<>(evidence.stream().filter(e -> cited.contains(e.getId()))
                .map(Evidence::getContent).toList());
        // kết quả Java tính cũng là nguồn hợp lệ của con số
        calculations.stream().filter(EvidenceCalculator.Calculation::ok).forEach(c -> citedSources.add(c.result()));
        List<String> badNumbers = CitationVerifier.unverifiedNumbers(answer, citedSources);
        int promptTokens = calls.stream().mapToInt(Draft::promptTokens).sum();
        int completionTokens = calls.stream().mapToInt(Draft::completionTokens).sum();
        return new AnswerResult(p.path("status").asText("not_found"), answer, citations, p.path("missing").asText(""),
                badNumbers, unknown, mode, promptTokens, completionTokens,
                llmProperties.cost(promptTokens, completionTokens), (System.nanoTime() - start) / 1_000_000, calculations);
    }

    private static String shorten(String s) {
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    private record Draft(JsonNode parsed, int promptTokens, int completionTokens) {
    }
}
