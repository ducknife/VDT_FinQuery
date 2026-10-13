package com.vdt.finq.modules.agent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.vdt.finq.common.exception.InvalidRequestException;
import com.vdt.finq.common.security.CurrentUser;
import com.vdt.finq.modules.agent.dto.AskRequest;
import com.vdt.finq.modules.agent.events.AgentEvent;
import com.vdt.finq.modules.agent.loop.QueryLoop;
import com.vdt.finq.modules.conversation.ConversationService;
import com.vdt.finq.modules.conversation.FeedbackService;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// hỏi đáp: POST /api/v1/ask trả SSE (các bước đang làm, chữ của câu trả lời, kết quả kiểm, done);
// POST /api/v1/ask/sync trả nguyên kết quả một lần (cho test, công cụ)
@Slf4j
@RestController
@RequestMapping("/api/v1/ask")
@RequiredArgsConstructor
public class AskController {

    private static final int MAX_QUESTION_CHARS = 1000;
    private static final long SSE_TIMEOUT_MS = 180_000;

    private final QueryLoop queryLoop;
    private final ConversationService conversationService;
    private final FeedbackService feedbackService;
    // mỗi câu hỏi một luồng ảo: chủ yếu chờ API nên không cần pool lớn
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(@RequestBody AskRequest request) {
        String question = validate(request);
        // đọc người dùng trên luồng của request: luồng nền không có SecurityContext
        CurrentUser user = CurrentUser.get();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        executor.execute(() -> {
            try {
                String context = context(request.conversationId(), user, question, emitter);
                AskResult result = queryLoop.ask(question, context, e -> send(emitter, e));
                ConversationService.Saved saved = conversationService.record(request.conversationId(), user, question, result);
                send(emitter, AgentEvent.of("saved", "conversationId", saved.conversationId(), "messageId", saved.messageId()));
                send(emitter, new AgentEvent("done", Map.of("result", result)));
                emitter.complete();
            } catch (RuntimeException e) {
                log.warn("hỏi đáp lỗi: {}", e.getMessage(), e);
                try {
                    send(emitter, AgentEvent.of("error", "message", String.valueOf(e.getMessage())));
                } catch (RuntimeException ignored) {
                    // bên nhận đã đóng
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    @PostMapping("/sync")
    public AskResult askSync(@RequestBody AskRequest request) {
        return queryLoop.ask(validate(request));
    }

    // lịch sử hội thoại (đã compaction) + bài học từ feedback câu tương tự; báo cho giao diện biết đã dùng
    private String context(Long conversationId, CurrentUser user, String question, SseEmitter emitter) {
        String history = conversationService.contextFor(conversationId, user);
        String lessons = feedbackService.lessonsFor(question);
        StringBuilder s = new StringBuilder();
        if (!history.isBlank()) {
            send(emitter, AgentEvent.of("step", "phase", "history"));
            s.append("Conversation so far (use it to resolve references such as \"năm trước\", \"chỉ tiêu đó\"; it is NOT ")
                    .append("evidence):\n").append(history);
        }
        if (!lessons.isBlank()) {
            send(emitter, AgentEvent.of("step", "phase", "lessons", "text", lessons));
            s.append(s.length() > 0 ? "\n\n" : "").append("Lessons from earlier feedback on similar questions (guidance on ")
                    .append("how to look for evidence; never a source of facts):\n").append(lessons);
        }
        return s.toString();
    }

    private static String validate(AskRequest request) {
        String q = request == null || request.question() == null ? "" : request.question().strip();
        if (q.isEmpty()) {
            throw new InvalidRequestException("câu hỏi trống");
        }
        if (q.length() > MAX_QUESTION_CHARS) {
            throw new InvalidRequestException("câu hỏi dài quá " + MAX_QUESTION_CHARS + " ký tự");
        }
        return q;
    }

    private static void send(SseEmitter emitter, AgentEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.type()).data(event.data(), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
