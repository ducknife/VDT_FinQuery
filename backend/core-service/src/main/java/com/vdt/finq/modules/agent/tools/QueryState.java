package com.vdt.finq.modules.agent.tools;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.model.ToolContext;

import com.vdt.finq.modules.agent.evidence.EvidenceStore;

import lombok.Getter;
import lombok.Setter;

// trạng thái một câu hỏi, truyền vào tool qua ToolContext (không gửi LLM)
@Getter
@Setter
public class QueryState {

    public static final String KEY = "finq.queryState";

    private final EvidenceStore evidence = new EvidenceStore();
    // decline_out_of_scope đã được gọi: lý do từ chối
    private String outOfScopeReason;
    private final List<String> trace = new ArrayList<>();
    private long toolMillis;
    private double toolCost;

    public void record(String line, long millis, double cost) {
        trace.add(line);
        toolMillis += millis;
        toolCost += cost;
    }

    public static QueryState of(ToolContext context) {
        Object state = context == null ? null : context.getContext().get(KEY);
        if (!(state instanceof QueryState s)) {
            throw new IllegalStateException("thiếu QueryState trong ToolContext");
        }
        return s;
    }
}
