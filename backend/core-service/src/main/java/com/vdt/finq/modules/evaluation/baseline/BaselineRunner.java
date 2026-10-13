package com.vdt.finq.modules.evaluation.baseline;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.properties.LlmProperties;
import com.vdt.finq.modules.agent.AskResult;
import com.vdt.finq.modules.agent.ModelOptions;
import com.vdt.finq.modules.agent.evidence.EvidenceStore;
import com.vdt.finq.modules.agent.loop.QueryLoop;
import com.vdt.finq.modules.agent.prompts.SystemPrompt;
import com.vdt.finq.modules.rag.generation.AnswerGenerator;
import com.vdt.finq.modules.rag.generation.AnswerResult;
import com.vdt.finq.modules.rag.retrieval.RetrievalOptions;
import com.vdt.finq.modules.rag.retrieval.RetrievalResult;
import com.vdt.finq.modules.rag.retrieval.RetrievalService;

// các đối chứng, cùng model với FinQuery: text-to-SQL, LLM không công cụ (nhánh số liệu); RAG cơ bản (nhánh tài liệu)
@Component
public class BaselineRunner {

    private final QueryLoop queryLoop;
    private final ChatModel chatModel;
    private final LlmProperties llmProperties;
    private final ToolCallback[] sqlTools;
    private final RetrievalService retrievalService;
    private final AnswerGenerator answerGenerator;

    public BaselineRunner(QueryLoop queryLoop, ChatModel chatModel, LlmProperties llmProperties, ExecuteSql executeSql,
            RetrievalService retrievalService, AnswerGenerator answerGenerator) {
        this.queryLoop = queryLoop;
        this.chatModel = chatModel;
        this.llmProperties = llmProperties;
        this.sqlTools = ToolCallbacks.from(executeSql);
        this.retrievalService = retrievalService;
        this.answerGenerator = answerGenerator;
    }

    public AskResult textToSql(String question) {
        return queryLoop.ask(question, BaselinePrompts.TEXT_TO_SQL, sqlTools);
    }

    public record PlainAnswer(String answer, long millis, int promptTokens, int completionTokens, double cost) {
    }

    // RAG cơ bản (đối chứng nhánh tài liệu): nguyên câu hỏi -> vector top 20, không agent, không lọc, không rerank;
    // cùng bước trả lời và bộ kiểm với FinQuery
    public AskResult basicRag(String question) {
        long start = System.nanoTime();
        try {
            RetrievalResult retrieval = retrievalService.retrieve(question, RetrievalOptions.BASIC_RAG);
            EvidenceStore evidence = new EvidenceStore();
            evidence.addChunks(retrieval.getChunks());
            long toolMillis = retrieval.getTimingsMs().getOrDefault("total", 0L);
            AnswerResult a = answerGenerator.generate(question, question, evidence.all());
            Map<String, Long> timings = new LinkedHashMap<>();
            timings.put("agent", 0L);
            timings.put("tool", toolMillis);
            timings.put("answer", a.getMillis());
            timings.put("total", (System.nanoTime() - start) / 1_000_000);
            Map<String, Double> cost = new LinkedHashMap<>();
            cost.put("agent", 0.0);
            cost.put("rerank", retrieval.getRerankCost());
            cost.put("answer", a.getCost());
            cost.put("total", retrieval.getRerankCost() + a.getCost());
            return new AskResult(question, a.getStatus(), a.getAnswer(), a.getCitations(), a.getMissing(),
                    a.getUnverifiedNumbers(), a.getUnknownIds(), 0,
                    List.of("vector top " + retrieval.getChunks().size() + " (RAG cơ bản)"), timings, cost,
                    a.getPromptTokens(), a.getCompletionTokens(), SystemPrompt.VERSION, llmProperties.getModel());
        } catch (IOException e) {
            throw new AppException(502, "lỗi bước trả lời: " + e.getMessage());
        }
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
