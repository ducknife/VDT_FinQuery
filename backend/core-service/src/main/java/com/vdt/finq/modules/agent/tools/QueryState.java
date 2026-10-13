package com.vdt.finq.modules.agent.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.ai.chat.model.ToolContext;

import com.vdt.finq.modules.agent.events.AgentEvent;
import com.vdt.finq.modules.agent.evidence.EvidenceStore;

import lombok.Getter;
import lombok.Setter;

// trạng thái một câu hỏi, truyền vào tool qua ToolContext (không gửi LLM)
@Getter
@Setter
public class QueryState {

    public static final String KEY = "finq.queryState";

    private final EvidenceStore evidence = new EvidenceStore();
    // câu người dùng gõ: lần tìm tài liệu đầu tiên dùng câu này (cấu hình đã benchmark), không dùng câu agent viết lại
    private String question;
    private int searches;
    // decline_out_of_scope đã được gọi: lý do từ chối
    private String outOfScopeReason;
    private final List<String> trace = new ArrayList<>();
    private long toolMillis;
    private double toolCost;
    // kênh sự kiện cho giao diện (SSE); mặc định bỏ qua
    private Consumer<AgentEvent> events = e -> {
    };

    public void record(String line, long millis, double cost) {
        trace.add(line);
        toolMillis += millis;
        toolCost += cost;
        emit(AgentEvent.of("tool_result", "text", line, "ms", millis));
    }

    // lỗi ở giao diện không được làm hỏng câu trả lời
    public void emit(AgentEvent event) {
        try {
            events.accept(event);
        } catch (RuntimeException ignored) {
            // bên nhận đã đóng kết nối
        }
    }

    public static QueryState of(ToolContext context) {
        Object state = context == null ? null : context.getContext().get(KEY);
        if (!(state instanceof QueryState s)) {
            throw new IllegalStateException("thiếu QueryState trong ToolContext");
        }
        return s;
    }
}
