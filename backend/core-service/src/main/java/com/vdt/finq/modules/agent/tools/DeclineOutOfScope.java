package com.vdt.finq.modules.agent.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

// từ chối câu hỏi ngoài phạm vi; ghi lý do để trả lời người dùng mà không bịa
@Component
public class DeclineOutOfScope {

    @Tool(name = "decline_out_of_scope", description = """
            Declare that the question is out of scope: another company, stock price forecasts, investment advice,
            topics unrelated to the company's documents, or periods that are not in the store.""")
    public String declineOutOfScope(
            @ToolParam(description = "Short reason in Vietnamese, shown to the user.") String reason,
            ToolContext toolContext) {
        QueryState state = QueryState.of(toolContext);
        String r = reason == null || reason.isBlank() ? "Câu hỏi nằm ngoài phạm vi tài liệu của công ty." : reason.strip();
        state.setOutOfScopeReason(r);
        state.record("decline_out_of_scope(\"" + r + "\")", 0, 0);
        return "Recorded as out of scope. Do not call other tools.";
    }
}
