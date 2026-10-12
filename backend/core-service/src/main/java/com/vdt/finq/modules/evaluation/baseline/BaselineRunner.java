package com.vdt.finq.modules.evaluation.baseline;

import java.util.List;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import com.vdt.finq.common.properties.LlmProperties;
import com.vdt.finq.modules.agent.AskResult;
import com.vdt.finq.modules.agent.ModelOptions;
import com.vdt.finq.modules.agent.loop.QueryLoop;

// chạy hai đối chứng của nhánh số liệu, cùng model với FinQuery:
// B text-to-SQL (cùng vòng agent, tool execute_sql), C LLM không công cụ (một lần gọi)
@Component
public class BaselineRunner {

    private final QueryLoop queryLoop;
    private final ChatModel chatModel;
    private final LlmProperties llmProperties;
    private final ToolCallback[] sqlTools;

    public BaselineRunner(QueryLoop queryLoop, ChatModel chatModel, LlmProperties llmProperties, ExecuteSql executeSql) {
        this.queryLoop = queryLoop;
        this.chatModel = chatModel;
        this.llmProperties = llmProperties;
        this.sqlTools = ToolCallbacks.from(executeSql);
    }

    public AskResult textToSql(String question) {
        return queryLoop.ask(question, BaselinePrompts.TEXT_TO_SQL, sqlTools);
    }

    public record PlainAnswer(String answer, long millis, int promptTokens, int completionTokens, double cost) {
    }

    public PlainAnswer llmOnly(String question) {
        long start = System.nanoTime();
        ChatResponse r = chatModel.call(new Prompt(List.of(new SystemMessage(BaselinePrompts.LLM_ONLY),
                new UserMessage(question)), ModelOptions.answer(llmProperties)));
        int in = ModelOptions.promptTokens(r);
        int out = ModelOptions.completionTokens(r);
        String text = r.getResult().getOutput().getText();
        return new PlainAnswer(text == null ? "" : text, (System.nanoTime() - start) / 1_000_000, in, out,
                llmProperties.cost(in, out));
    }
}
