package com.vdt.finq.modules.evaluation;

import java.util.List;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.properties.LlmProperties;
import com.vdt.finq.modules.agent.ModelOptions;

import lombok.RequiredArgsConstructor;

// chấm "đúng ý" câu trả lời nhánh tài liệu so với đáp án vàng: đúng / một phần / sai, kèm lý do.
// chỉ là lượt chấm đầu; người làm đề tài duyệt lại các câu sai, một phần và vài câu đúng ngẫu nhiên
@Component
@RequiredArgsConstructor
public class AnswerJudge {

    private static final String PROMPT = """
            You grade an answer to a question about a Vietnamese company's financial documents against a
            REFERENCE answer written by an expert. Judge only the factual content against the reference:
            - correct: contains the key facts and figures of the reference (wording may differ, extra correct
              details are fine) and states nothing that contradicts it.
            - partial: some key facts of the reference are present but others are missing, or it is vague.
            - wrong: the key facts are missing or contradicted (wrong number, period or scope), or it refuses
              or says "not found" although the reference has an answer.
            Numbers must match the reference when stated; rounding to the reference's precision is fine.
            Ignore citation markers such as [C1] or [D2].
            Reply with ONLY a JSON object: {"verdict": "correct" | "partial" | "wrong", "reason": "one short
            sentence in Vietnamese"}
            """;

    private final ChatModel chatModel;
    private final LlmProperties llmProperties;
    private final ObjectMapper json;

    public record Judgment(String verdict, String reason, double cost) {
    }

    public Judgment judge(String question, String reference, String answer) {
        String user = "QUESTION:\n" + question + "\n\nREFERENCE ANSWER:\n" + reference + "\n\nANSWER TO GRADE:\n"
                + (answer == null || answer.isBlank() ? "(empty)" : answer);
        ChatResponse r = chatModel.call(new Prompt(List.of(new SystemMessage(PROMPT), new UserMessage(user)),
                ModelOptions.answer(llmProperties)));
        double cost = llmProperties.cost(ModelOptions.promptTokens(r), ModelOptions.completionTokens(r));
        String text = r.getResult().getOutput().getText();
        try {
            String body = text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1);
            JsonNode n = json.readTree(body);
            return new Judgment(n.path("verdict").asText("wrong"), n.path("reason").asText(""), cost);
        } catch (Exception e) {
            return new Judgment("wrong", "không đọc được kết quả chấm: " + text, cost);
        }
    }
}
