package com.vdt.finq.modules.agent.events;

import java.util.Map;

// một sự kiện "hệ thống đang làm gì", gửi cho giao diện qua SSE.
// type: step | plan | tool | tool_result | retrieval | sources | answer_delta | verification | done | error
public record AgentEvent(String type, Map<String, Object> data) {

    public static AgentEvent of(String type, Object... keyValues) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            m.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return new AgentEvent(type, m);
    }
}
