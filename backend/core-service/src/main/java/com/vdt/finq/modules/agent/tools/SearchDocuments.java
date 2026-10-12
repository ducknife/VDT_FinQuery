package com.vdt.finq.modules.agent.tools;

import java.util.List;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.vdt.finq.modules.agent.evidence.Evidence;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievalOptions;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievalResult;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievalService;

import lombok.RequiredArgsConstructor;

// nhánh RAG: chạy truy hồi đầy đủ; trả LLM danh sách ngắn, toàn văn nằm trong EvidenceStore
@Component
@RequiredArgsConstructor
public class SearchDocuments {

    private static final int PREVIEW_CHARS = 350;

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
        String q = query == null ? "" : query.strip();
        if (q.isEmpty()) {
            return "Error: the query is empty.";
        }
        RetrievalResult result = retrievalService.retrieve(q, RetrievalOptions.DEFAULT);
        List<Evidence> added = state.getEvidence().addChunks(result.getChunks());
        state.record("search_documents(\"" + q + "\") -> " + added.size() + " chunk, lọc " + result.getFilterMode(),
                result.getTimingsMs().getOrDefault("total", 0L), result.getRerankCost());
        if (added.isEmpty()) {
            return "No passages found for: " + q;
        }
        StringBuilder out = new StringBuilder("Found " + added.size() + " passages (ranked, most relevant first). "
                + "The answer step reads their FULL text. Below is only the beginning of each page (often just the "
                + "title), so a preview that does not show the answer does NOT mean the answer is missing:\n");
        for (Evidence e : added) {
            String flat = e.getContent().replaceAll("\\s+", " ").strip();
            out.append('[').append(e.getId()).append("] ").append(e.source()).append(": ")
                    .append(flat.length() > PREVIEW_CHARS ? flat.substring(0, PREVIEW_CHARS) + "…" : flat).append('\n');
        }
        return out.toString();
    }
}
