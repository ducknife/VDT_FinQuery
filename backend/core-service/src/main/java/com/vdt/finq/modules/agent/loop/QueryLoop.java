package com.vdt.finq.modules.agent.loop;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.properties.AgentProperties;
import com.vdt.finq.common.properties.LlmProperties;
import com.vdt.finq.modules.agent.AskResult;
import com.vdt.finq.modules.agent.ModelOptions;
import com.vdt.finq.modules.agent.events.AgentEvent;
import com.vdt.finq.modules.agent.events.ToolCallText;
import com.vdt.finq.modules.agent.prompts.SystemPrompt;
import com.vdt.finq.modules.agent.tools.QueryState;
import com.vdt.finq.modules.dbquery.support.FigureLookup;
import com.vdt.finq.modules.dbquery.support.IndicatorCatalog;
import com.vdt.finq.modules.dbquery.support.ReportPeriod;
import com.vdt.finq.modules.dbquery.support.Scope;
import com.vdt.finq.modules.rag.generation.AnswerGenerator;
import com.vdt.finq.modules.rag.generation.AnswerResult;
import com.vdt.finq.modules.rag.preretrieval.QueryPreprocessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// vòng agent cho một câu hỏi (như VDT_AIOps): tối đa max-turn-loop - 1 lượt chọn tool,
// rồi một lượt viết câu trả lời từ bằng chứng; chỉ dừng theo số lượt
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryLoop {

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final ToolCallback[] agentTools;
    private final AnswerGenerator answerGenerator;
    private final QueryPreprocessor preprocessor;
    private final AgentProperties agentProperties;
    private final LlmProperties llmProperties;
    private final IndicatorCatalog indicatorCatalog;
    private final FigureLookup figureLookup;
    private final ObjectMapper objectMapper;

    public AskResult ask(String question) {
        return ask(question, SystemPrompt.AGENT_SYSTEM_PROMPT, agentTools);
    }

    // như ask nhưng phát sự kiện cho giao diện (SSE) và stream chữ của câu trả lời
    public AskResult ask(String question, Consumer<AgentEvent> listener) {
        return ask(question, "", listener);
    }

    // context: lịch sử hội thoại (đã compaction) và bài học từ feedback câu tương tự; không phải bằng chứng
    public AskResult ask(String question, String context, Consumer<AgentEvent> listener) {
        return ask(question, SystemPrompt.AGENT_SYSTEM_PROMPT, agentTools, listener, context);
    }

    // cùng vòng lặp và bước trả lời, khác prompt và bộ tool: dùng cho đối chứng (text-to-SQL)
    public AskResult ask(String question, String agentSystemPrompt, ToolCallback[] tools) {
        return ask(question, agentSystemPrompt, tools, null, "");
    }

    private AskResult ask(String question, String agentSystemPrompt, ToolCallback[] tools, Consumer<AgentEvent> listener,
            String context) {
        long start = System.nanoTime();
        QueryState state = new QueryState();
        state.setQuestion(question);
        if (listener != null) {
            state.setEvents(listener);
        }
        // bản chuẩn hoá chỉ để LLM hiểu rõ kỳ, phạm vi; tool tự chuẩn hoá câu truy vấn của nó
        String normalized = preprocessor.prepare(question, false).getNormalized().getText();
        state.emit(AgentEvent.of("step", "phase", "understand", "normalized", normalized));
        OpenAiChatOptions options = ModelOptions.withTools(llmProperties, tools, Map.of(QueryState.KEY, state));
        // danh sách mã chỉ tiêu gửi một lần ở đây; kỳ mới nhất để hiểu "kỳ gần nhất", "quý trước"
        LocalDate latest = figureLookup.latest(Scope.CONSOLIDATED);
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(agentSystemPrompt + indicatorCatalog.promptSection()),
                new UserMessage("Question (as typed): " + question + "\nNormalized: " + normalized
                        + "\nLatest period in the figures database: " + (latest == null ? "unknown" : ReportPeriod.date(latest))
                        + (context == null || context.isBlank() ? "" : "\n\n" + context.strip()))),
                options);

        int maxToolRounds = Math.max(1, agentProperties.getMaxTurnLoop() - 1);
        int rounds = 0;
        long agentMillis = 0;
        int promptTokens = 0;
        int completionTokens = 0;
        try {
            while (true) {
                long t = System.nanoTime();
                state.emit(AgentEvent.of("step", "phase", "agent_round", "round", rounds + 1));
                ChatResponse response = chatModel.call(prompt);
                rounds++;
                agentMillis += (System.nanoTime() - t) / 1_000_000;
                promptTokens += ModelOptions.promptTokens(response);
                completionTokens += ModelOptions.completionTokens(response);
                log.info("agent lượt {}: gọi tool = {}, thinking {} ký tự, token vào {} ra {}, {} ms", rounds,
                        response.hasToolCalls(), ModelOptions.reasoningChars(response), ModelOptions.promptTokens(response),
                        ModelOptions.completionTokens(response), (System.nanoTime() - t) / 1_000_000);
                // ghi chú của model và tool nó chọn: để hiểu vì sao nó tìm thêm hay dừng
                log.info("agent lượt {} ghi chú: {} | tool: {}", rounds, response.getResult().getOutput().getText(),
                        response.getResult().getOutput().getToolCalls().stream()
                                .map(c -> c.name() + c.arguments()).toList());
                // lời của agent (kế hoạch trước khi gọi tool, hoặc ghi chú khi dừng) và các tool nó gọi
                String note = response.getResult().getOutput().getText();
                if (note != null && !note.isBlank()) {
                    state.emit(AgentEvent.of("plan", "text", note.strip()));
                }
                response.getResult().getOutput().getToolCalls().forEach(c -> state.emit(AgentEvent.of("tool", "name", c.name(),
                        "text", ToolCallText.describe(c.name(), c.arguments(), objectMapper))));
                if (!response.hasToolCalls()) {
                    break;
                }
                ToolExecutionResult exec;
                try {
                    exec = toolCallingManager.executeToolCalls(prompt, response);
                } catch (RuntimeException e) {
                    // model gọi tool không tồn tại hoặc tool lỗi: ghi lại, trả lời bằng bằng chứng đã có
                    log.warn("chạy tool lỗi: {}", e.getMessage());
                    state.record("LỖI khi chạy tool: " + e.getMessage(), 0, 0);
                    break;
                }
                // từ chối, hoặc đã hết số lượt chọn tool: không hỏi LLM thêm, chuyển sang bước trả lời
                if (state.getOutOfScopeReason() != null || rounds >= maxToolRounds) {
                    break;
                }
                prompt = new Prompt(exec.conversationHistory(), options);
            }

            state.emit(AgentEvent.of("step", "phase", "answer", "evidence", state.getEvidence().all().size()));
            // bằng chứng đưa sang bước trả lời: để soát câu trả lời sai do thiếu nguồn hay do đọc sót
            log.info("bằng chứng cho bước trả lời: {}", state.getEvidence().all().stream()
                    .map(e -> e.getId() + "=" + (e.getTaiLieuId() == null ? "tính" : e.getTaiLieuId() + "#" + e.getTrang()))
                    .toList());
            AnswerResult answer = state.getOutOfScopeReason() != null
                    ? new AnswerResult("out_of_scope", "Thưa đồng chí, câu hỏi này nằm ngoài phạm vi FinQuery: "
                            + state.getOutOfScopeReason(), List.of(), "", List.of(), List.of(), "none", 0, 0, 0, 0, List.of())
                    : answerGenerator.generate(question, normalized, state.getEvidence().all(),
                            listener == null ? null : delta -> state.emit(AgentEvent.of("answer_delta", "text", delta)),
                            context);
            if (listener != null && "out_of_scope".equals(answer.getStatus())) {
                state.emit(AgentEvent.of("answer_delta", "text", answer.getAnswer()));
            }
            // có phép tính: chữ đã stream còn chỗ {{K#}}, gửi bản đã điền kết quả để giao diện thay
            if (!answer.getCalculations().isEmpty()) {
                log.info("phép tính ở bước trả lời: {}", answer.getCalculations());
                state.emit(AgentEvent.of("answer_final", "text", answer.getAnswer()));
            }
            // kết quả kiểm sau khi viết xong: từng trích dẫn có thấy trong nguồn không, số nào không có nguồn
            state.emit(AgentEvent.of("verification", "status", answer.getStatus(), "citations", answer.getCitations(),
                    "unverifiedNumbers", answer.getUnverifiedNumbers(), "unknownIds", answer.getUnknownIds(),
                    "missing", answer.getMissing(), "calculations", answer.getCalculations()));

            double agentCost = llmProperties.cost(promptTokens, completionTokens);
            Map<String, Long> timings = new LinkedHashMap<>();
            timings.put("agent", agentMillis);
            timings.put("tool", state.getToolMillis());
            timings.put("answer", answer.getMillis());
            timings.put("total", (System.nanoTime() - start) / 1_000_000);
            Map<String, Double> cost = new LinkedHashMap<>();
            cost.put("agent", agentCost);
            cost.put("rerank", state.getToolCost());
            cost.put("answer", answer.getCost());
            cost.put("total", agentCost + state.getToolCost() + answer.getCost());
            return new AskResult(question, answer.getStatus(), answer.getAnswer(), answer.getCitations(),
                    answer.getMissing(), answer.getUnverifiedNumbers(), answer.getUnknownIds(), rounds,
                    List.copyOf(state.getTrace()), timings, cost, promptTokens + answer.getPromptTokens(),
                    completionTokens + answer.getCompletionTokens(), SystemPrompt.VERSION, llmProperties.getModel());
        } catch (IOException e) {
            throw new AppException(502, "lỗi bước trả lời: " + e.getMessage());
        }
    }
}
