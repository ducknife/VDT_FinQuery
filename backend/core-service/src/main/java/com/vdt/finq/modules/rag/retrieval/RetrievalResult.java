package com.vdt.finq.modules.rag.retrieval;

import java.util.List;
import java.util.Map;

import com.vdt.finq.modules.rag.preretrieval.PreparedQuery;
import com.vdt.finq.modules.rag.retrieval.rerank.TierStat;

import lombok.AllArgsConstructor;
import lombok.Getter;

// kết quả truy hồi một câu hỏi: top chunk + số đo từng bước (cho gỡ lỗi, benchmark, bảng chi phí)
@Getter
@AllArgsConstructor
public class RetrievalResult {
    private final String question;
    private final RetrievalOptions options;
    private final PreparedQuery prepared;
    // "strict" | "relaxed" (lọc chặt ra quá ít nên nới) | "off"
    private final String filterMode;
    private final int qdrantHits;
    private final List<TierStat> tiers;
    private final List<RetrievedChunk> chunks;
    // thời gian từng bước, mili giây: preprocess, embed, search, rerank, total
    private final Map<String, Long> timingsMs;
    // tiền rerank thật (USD) do DeepInfra trả về; embed câu hỏi gần như 0 nên không tính
    private final double rerankCost;
    // thứ hạng ở từng tầng (chỉ toạ độ), để đo Recall@80 và Recall@40
    private final List<ChunkRef> qdrantRanking;
    private final List<ChunkRef> tier1Ranking;
}
