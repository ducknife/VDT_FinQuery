package com.vdt.finq.modules.agent.dto;

// câu hỏi gửi lên POST /api/v1/ask; conversationId rỗng là hội thoại mới
public record AskRequest(String question, Long conversationId) {
}
