package com.vdt.finq.modules.conversation;

import java.util.List;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.properties.LlmProperties;
import com.vdt.finq.common.security.CurrentUser;
import com.vdt.finq.modules.agent.AskResult;
import com.vdt.finq.modules.agent.ModelOptions;
import com.vdt.finq.modules.conversation.ConversationRepository.ConversationRow;
import com.vdt.finq.modules.conversation.ConversationRepository.MessageRow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// lịch sử hội thoại và compaction: gửi kèm KEEP lượt gần nhất nguyên văn; lượt cũ hơn gộp vào bản tóm tắt
// (giữ đúng số, kỳ, phạm vi, nguồn), tóm tắt dần mỗi khi có lượt mới rơi ra khỏi cửa sổ
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int KEEP = 3;
    private static final int ANSWER_CHARS = 1200;
    private static final String SUMMARY_PROMPT = """
            Summarize the earlier turns of a conversation between a finance employee and FinQuery, an assistant
            for Viettel Construction (CTR) financial documents. Write in Vietnamese, at most 150 words. Keep every
            figure exactly as written, with its period, scope (hợp nhất / riêng) and source. Keep what the user was
            interested in. Do not add any new fact.
            """;

    public record Saved(long conversationId, long messageId) {
    }

    private final ConversationRepository repository;
    private final ChatModel chatModel;
    private final LlmProperties llmProperties;
    private final ObjectMapper json;

    // ngữ cảnh gửi kèm câu hỏi mới; rỗng nếu là hội thoại mới
    public String contextFor(Long conversationId, CurrentUser user) {
        if (conversationId == null) {
            return "";
        }
        ConversationRow c = repository.findConversation(conversationId, user.id())
                .orElseThrow(() -> new AppException(404, "không có hội thoại " + conversationId));
        List<MessageRow> turns = repository.messages(c.id());
        int recentFrom = Math.max(0, turns.size() - KEEP);
        String summary = c.tomTat();
        if (recentFrom > c.soLuotTomTat()) {
            summary = summarize(summary, turns.subList(c.soLuotTomTat(), recentFrom));
            repository.updateSummary(c.id(), summary, recentFrom);
        }
        StringBuilder s = new StringBuilder();
        if (summary != null && !summary.isBlank()) {
            s.append("Tóm tắt các lượt trước: ").append(summary.strip()).append('\n');
        }
        for (MessageRow m : turns.subList(recentFrom, turns.size())) {
            s.append("Người dùng hỏi: ").append(m.cauHoi()).append('\n').append("FinQuery trả lời: ")
                    .append(shorten(m.traLoi())).append('\n');
        }
        return s.toString().strip();
    }

    public Saved record(Long conversationId, CurrentUser user, String question, AskResult result) {
        long id = conversationId != null ? conversationId
                : repository.createConversation(user.id(), question.length() > 180 ? question.substring(0, 180) + "…" : question);
        String body;
        try {
            body = json.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            body = null;
        }
        long messageId = repository.addMessage(id, question, result.getAnswer(), result.getStatus(), body);
        return new Saved(id, messageId);
    }

    // tóm tắt dần: bản cũ + các lượt vừa rơi khỏi cửa sổ
    private String summarize(String previous, List<MessageRow> turns) {
        StringBuilder s = new StringBuilder();
        if (previous != null && !previous.isBlank()) {
            s.append("PREVIOUS SUMMARY:\n").append(previous).append("\n\n");
        }
        s.append("TURNS TO ADD:\n");
        for (MessageRow m : turns) {
            s.append("Q: ").append(m.cauHoi()).append("\nA: ").append(shorten(m.traLoi())).append("\n\n");
        }
        try {
            String out = chatModel.call(new Prompt(List.of(new SystemMessage(SUMMARY_PROMPT), new UserMessage(s.toString())),
                    ModelOptions.answer(llmProperties))).getResult().getOutput().getText();
            log.info("compaction: gộp {} lượt vào tóm tắt", turns.size());
            return out == null ? previous : out.strip();
        } catch (RuntimeException e) {
            // tóm tắt lỗi thì giữ bản cũ, không làm hỏng câu hỏi hiện tại
            log.warn("compaction lỗi: {}", e.getMessage());
            return previous;
        }
    }

    private static String shorten(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > ANSWER_CHARS ? s.substring(0, ANSWER_CHARS) + "…" : s;
    }
}
