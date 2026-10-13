package com.vdt.finq.modules.agent;

import java.util.Map;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;

import com.vdt.finq.common.properties.LlmProperties;

// tuỳ chọn gọi model (thinking, tool) và đọc số đo từ phản hồi
public final class ModelOptions {

    private ModelOptions() {
    }

    // vòng agent: gửi tool, tắt tự chạy tool, trạng thái đi qua ToolContext
    public static OpenAiChatOptions withTools(LlmProperties llm, ToolCallback[] tools, Map<String, Object> toolContext) {
        return OpenAiChatOptions.builder()
                .toolCallbacks(tools)
                .internalToolExecutionEnabled(false)
                .toolContext(toolContext)
                .extraBody(thinking(llm))
                .build();
    }

    // bước trả lời: không có tool
    public static OpenAiChatOptions answer(LlmProperties llm) {
        return OpenAiChatOptions.builder().extraBody(thinking(llm)).build();
    }

    // bước trả lời dạng stream: xin kèm usage ở mẩu cuối để vẫn tính được token và tiền
    public static OpenAiChatOptions answerStreaming(LlmProperties llm) {
        return OpenAiChatOptions.builder().extraBody(thinking(llm)).streamUsage(true).build();
    }

    // chat_template_kwargs chỉ dành cho Qwen3.x; model khác gọi mặc định
    private static Map<String, Object> thinking(LlmProperties llm) {
        if (llm.getModel() == null || !llm.getModel().startsWith("Qwen/")) {
            return Map.of();
        }
        return Map.of("chat_template_kwargs", Map.of("enable_thinking", llm.isThinking()));
    }

    public static int promptTokens(ChatResponse response) {
        Usage u = response.getMetadata().getUsage();
        return u == null || u.getPromptTokens() == null ? 0 : u.getPromptTokens();
    }

    public static int completionTokens(ChatResponse response) {
        Usage u = response.getMetadata().getUsage();
        return u == null || u.getCompletionTokens() == null ? 0 : u.getCompletionTokens();
    }

    // độ dài phần nháp thinking (để ghi log, không trả cho người dùng)
    public static int reasoningChars(ChatResponse response) {
        Object r = response.getResult().getOutput().getMetadata().get("reasoningContent");
        return r == null ? 0 : r.toString().length();
    }
}
