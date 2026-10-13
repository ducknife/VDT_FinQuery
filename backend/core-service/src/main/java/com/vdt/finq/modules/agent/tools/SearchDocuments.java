package com.vdt.finq.modules.agent.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.vdt.finq.modules.agent.events.AgentEvent;
import com.vdt.finq.modules.agent.evidence.Evidence;
import com.vdt.finq.modules.rag.retrieval.RetrievalOptions;
import com.vdt.finq.modules.rag.retrieval.RetrievalResult;
import com.vdt.finq.modules.rag.retrieval.RetrievalService;

import lombok.RequiredArgsConstructor;

// nhánh RAG: chạy truy hồi đầy đủ; trả LLM danh sách ngắn, toàn văn nằm trong EvidenceStore
@Component
@RequiredArgsConstructor
public class SearchDocuments {

    private static final int PREVIEW_CHARS = 350;
    private static final int SOURCES_SHOWN = 5;

    private final RetrievalService retrievalService;

    @Tool(name = "search_documents", description = """
            Search the company's documents (financial statements, annual reports, shareholder meeting resolutions
            and materials, board resolutions, disclosures, explanation letters, business plans) and return the most
            relevant passages with their source document and page. Use it for explanations, plans and targets,
            dividends, capital changes, governance and people, strategy, risks, operating data, and for figures that
            are not standard financial statement line items.""")
    public String searchDocuments(
            @ToolParam(description = "A complete, self-contained search query in Vietnamese: what is asked, the "
                    + "period (year, quarter, half-year) and the scope (hợp nhất / riêng) when relevant.") String query,
            ToolContext toolContext) {
        QueryState state = QueryState.of(toolContext);
        String proposed = query == null ? "" : query.strip();
        // lần tìm đầu: câu hỏi gốc, vì truy hồi được đo và chỉnh trên câu người dùng gõ; câu agent viết lại
        // có thể đẩy trang đúng khỏi 80 ứng viên (B07, 13/10). từ lần thứ hai mới dùng câu agent viết
        boolean first = state.getSearches() == 0 && state.getQuestion() != null && !state.getQuestion().isBlank();
        String q = first ? state.getQuestion().strip() : proposed;
        state.setSearches(state.getSearches() + 1);
        if (q.isEmpty()) {
            return "Error: the query is empty.";
        }
        RetrievalResult result = retrievalService.retrieve(q, RetrievalOptions.DEFAULT,
                (step, data) -> state.emit(new AgentEvent("retrieval", withStep(step, data))));
        List<Evidence> added = state.getEvidence().addChunks(result.getChunks());
        state.record("search_documents(\"" + q + "\"" + (q.equals(proposed) ? "" : ", agent đề xuất \"" + proposed + "\"")
                + ") -> " + added.size() + " chunk, lọc " + result.getFilterMode(),
                result.getTimingsMs().getOrDefault("total", 0L), result.getRerankCost());
        // nguồn chính cho giao diện: 5 đoạn đầu (mã, tài liệu, trang, điểm)
        state.emit(AgentEvent.of("sources", "items", added.stream().limit(SOURCES_SHOWN)
                .map(e -> Map.of("id", e.getId(), "taiLieuId", e.getTaiLieuId(), "title", String.valueOf(e.getTenHienThi()),
                        "page", e.getTrang(), "score", e.getScore() == null ? 0 : e.getScore()))
                .toList()));
        if (added.isEmpty()) {
            return "No passages found for: " + q;
        }
        StringBuilder out = new StringBuilder(first && !q.equals(proposed)
                ? "This first search used the user's question as typed (\"" + q + "\"). If a part of the question is "
                        + "still not covered below, search again for that part with your own query.\n"
                : "");
        out.append("Found " + added.size() + " passages (ranked, most relevant first). "
                + "The answer step reads their FULL text. Below is only the beginning of each page (often just the "
                + "title), so a preview that does not show the answer does NOT mean the answer is missing:\n");
        for (Evidence e : added) {
            String flat = e.getContent().replaceAll("\\s+", " ").strip();
            out.append('[').append(e.getId()).append("] ").append(e.source()).append(": ")
                    .append(flat.length() > PREVIEW_CHARS ? flat.substring(0, PREVIEW_CHARS) + "…" : flat).append('\n');
        }
        return out.toString();
    }

    private static Map<String, Object> withStep(String step, Map<String, Object> data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("step", step);
        m.putAll(data);
        return m;
    }
}
