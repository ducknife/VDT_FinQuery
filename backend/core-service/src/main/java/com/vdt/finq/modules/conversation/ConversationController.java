package com.vdt.finq.modules.conversation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.exception.InvalidRequestException;
import com.vdt.finq.common.security.CurrentUser;
import com.vdt.finq.modules.conversation.ConversationRepository.ConversationRow;

import lombok.RequiredArgsConstructor;

// lịch sử hội thoại của người đang đăng nhập, feedback cho từng câu trả lời
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConversationController {

    private static final int LIST_LIMIT = 50;

    private final ConversationRepository repository;
    private final FeedbackService feedbackService;
    private final ObjectMapper json;

    public record FeedbackRequest(Long messageId, String level, String reason) {
    }

    // người đang đăng nhập, vai trò và việc được làm; gọi được cả khi chưa được duyệt (để giao diện báo "chờ duyệt")
    @GetMapping("/me")
    public Map<String, Object> me() {
        CurrentUser user = CurrentUser.get();
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", user.id());
        v.put("name", user.name());
        v.put("displayName", user.displayName());
        v.put("roles", user.roles());
        v.put("permissions", user.permissions());
        return v;
    }

    @GetMapping("/conversations")
    public List<Map<String, Object>> list() {
        return repository.listConversations(CurrentUser.get().id(), LIST_LIMIT).stream()
                .map(c -> Map.<String, Object>of("id", c.id(), "title", c.tieuDe(), "updatedAt", c.capNhatLuc().toString()))
                .toList();
    }

    @GetMapping("/conversations/{id}")
    public Map<String, Object> get(@PathVariable long id) {
        CurrentUser user = CurrentUser.get();
        ConversationRow c = repository.findConversation(id, user.id())
                .orElseThrow(() -> new AppException(404, "không có hội thoại " + id));
        List<Map<String, Object>> messages = repository.messages(id).stream().map(m -> {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", m.id());
            v.put("question", m.cauHoi());
            v.put("answer", m.traLoi());
            v.put("status", m.trangThai());
            v.put("result", parse(m.ketQuaJson()));
            v.put("feedback", repository.feedbackOf(m.id(), user.id()).orElse(null));
            return v;
        }).toList();
        return Map.of("id", c.id(), "title", c.tieuDe(), "messages", messages);
    }

    public record Rename(String title) {
    }

    @DeleteMapping("/conversations/{id}")
    public Map<String, Object> delete(@PathVariable long id) {
        if (!repository.deleteConversation(id, CurrentUser.get().id())) {
            throw new AppException(404, "không có hội thoại " + id);
        }
        return Map.of("id", id, "deleted", true);
    }

    @PatchMapping("/conversations/{id}")
    public Map<String, Object> rename(@PathVariable long id, @RequestBody Rename body) {
        String title = body == null || body.title() == null ? "" : body.title().strip();
        if (title.isEmpty() || title.length() > 200) {
            throw new InvalidRequestException("tên hội thoại phải có 1–200 ký tự");
        }
        if (!repository.renameConversation(id, CurrentUser.get().id(), title)) {
            throw new AppException(404, "không có hội thoại " + id);
        }
        return Map.of("id", id, "title", title);
    }

    @PostMapping("/feedback")
    public Map<String, Object> feedback(@RequestBody FeedbackRequest request) {
        if (request == null || request.messageId() == null) {
            throw new AppException(400, "thiếu messageId");
        }
        feedbackService.save(CurrentUser.get(), request.messageId(), request.level(), request.reason());
        return Map.of("messageId", request.messageId(), "level", request.level());
    }

    private JsonNode parse(String body) {
        try {
            return body == null ? null : json.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }
}
