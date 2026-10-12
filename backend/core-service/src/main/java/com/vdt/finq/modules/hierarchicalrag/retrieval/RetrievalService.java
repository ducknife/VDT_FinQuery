package com.vdt.finq.modules.hierarchicalrag.retrieval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.properties.RetrievalProperties;
import com.vdt.finq.common.text.EnglishFilter;
import com.vdt.finq.modules.document.Document;
import com.vdt.finq.modules.document.DocumentRepository;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.embedder.EmbeddingClient;
import com.vdt.finq.modules.hierarchicalrag.preretrieval.PreparedQuery;
import com.vdt.finq.modules.hierarchicalrag.preretrieval.QueryPreprocessor;
import com.vdt.finq.modules.hierarchicalrag.retrieval.rerank.HierarchicalReranker;
import com.vdt.finq.modules.hierarchicalrag.retrieval.rerank.TierStat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// truy hồi một câu hỏi: tiền truy xuất -> embed -> Qdrant 80 -> bỏ tiếng Anh -> rerank 40 -> 20
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final QueryPreprocessor preprocessor;
    private final EmbeddingClient embeddingClient;
    private final QdrantRetriever retriever;
    private final HierarchicalReranker reranker;
    private final DocumentRepository documentRepository;
    private final RetrievalProperties properties;

    public RetrievalResult retrieve(String question, RetrievalOptions options) {
        Map<String, Long> timings = new LinkedHashMap<>();
        long start = System.nanoTime();
        try {
            long t = System.nanoTime();
            PreparedQuery prepared = preprocessor.prepare(question, options.isKeywords());
            timings.put("preprocess", elapsed(t));

            // RAG cơ bản (đối chứng): dùng nguyên câu người dùng gõ, không qua chuẩn hoá và từ khoá
            String queryText = options.isNormalize() ? prepared.getText() : question;

            t = System.nanoTime();
            float[] vector = embeddingClient.embed(List.of(queryForEmbedding(queryText))).get(0);
            timings.put("embed", elapsed(t));

            t = System.nanoTime();
            // lấy 80 từ Qdrant, bỏ tiếng Anh xong còn bao nhiêu rerank bấy nhiêu; RAG cơ bản không lọc
            boolean dropEnglish = properties.isDropEnglish() && options.isNormalize();
            int searchK = properties.getTopK();
            String filterMode = "off";
            List<RetrievedChunk> hits;
            if (options.isFilter()) {
                filterMode = "strict";
                hits = retriever.search(vector, QdrantFilters.strict(prepared.getFilter()), searchK);
                // lọc sai năm/kỳ không được làm mất đáp án: ra quá ít thì chỉ giữ điều kiện công ty
                if (hits.size() < properties.getMinFilteredHits()) {
                    filterMode = "relaxed";
                    hits = retriever.search(vector, QdrantFilters.relaxed(prepared.getFilter()), searchK);
                }
            } else {
                hits = retriever.search(vector, null, searchK);
            }
            if (dropEnglish) {
                hits = dropEnglish(question, hits);
            }
            attachTitles(hits);
            timings.put("search", elapsed(t));

            t = System.nanoTime();
            List<ChunkRef> qdrantRanking = hits.stream().map(ChunkRef::of).toList();
            List<RetrievedChunk> chunks;
            List<TierStat> tiers;
            List<ChunkRef> tier1Ranking;
            if (options.isRerank()) {
                HierarchicalReranker.Outcome outcome = reranker.rerank(queryText, hits);
                chunks = outcome.getChunks();
                tiers = outcome.getTiers();
                tier1Ranking = outcome.getAfterTier1().stream().map(ChunkRef::of).toList();
            } else {
                chunks = List.copyOf(hits.subList(0, Math.min(properties.getFinalK(), hits.size())));
                tiers = List.of();
                tier1Ranking = qdrantRanking;
            }
            timings.put("rerank", elapsed(t));
            timings.put("total", elapsed(start));

            double cost = tiers.stream().mapToDouble(TierStat::getCost).sum();
            log.info("truy hồi \"{}\": lọc {}, Qdrant {} chunk -> {} chunk, {} ms, rerank {} USD",
                    question, filterMode, hits.size(), chunks.size(), timings.get("total"), cost);
            return new RetrievalResult(question, options, prepared, filterMode, hits.size(), tiers, chunks,
                    timings, cost, qdrantRanking, tier1Ranking);
        } catch (IOException e) {
            throw new AppException(502, "lỗi gọi embedding hoặc rerank: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(503, "truy hồi bị ngắt");
        }
    }

    // bỏ chunk thuần tiếng Anh (bản dịch); làm sạch đoạn tiếng Anh MỘT lần cho reranker và LLM
    private List<RetrievedChunk> dropEnglish(String question, List<RetrievedChunk> hits) {
        List<RetrievedChunk> kept = new ArrayList<>();
        int dropped = 0;
        int stripped = 0;
        for (RetrievedChunk c : hits) {
            if (EnglishFilter.isEnglishChunk(c.getNoiDung())) {
                dropped++;
                continue;
            }
            String clean = EnglishFilter.stripEnglish(c.getNoiDung());
            // chỉ đếm chunk thật sự bị bỏ đoạn (stripEnglish còn gộp dòng trống, không tính)
            if (clean.length() < c.getNoiDung().replaceAll("\n{3,}", "\n\n").strip().length()) {
                stripped++;
            }
            c.setNoiDungSach(clean);
            kept.add(c);
        }
        log.info("lọc tiếng Anh \"{}\": Qdrant {} chunk, bỏ {} chunk tiếng Anh, làm sạch {} chunk song ngữ, giữ {}",
                question, hits.size(), dropped, stripped, kept.size());
        return kept;
    }

    // Qwen3-Embedding khuyên thêm câu hướng dẫn cho PHÍA CÂU HỎI (chunk lúc nạp thì không thêm)
    private String queryForEmbedding(String text) {
        String instruction = properties.getQueryInstruction();
        if (instruction == null || instruction.isBlank()) {
            return text;
        }
        return "Instruct: " + instruction + "\nQuery:" + text;
    }

    // tên tài liệu nằm ở MySQL, không ở payload; reranker cần nó để chấm đúng bản đã embed
    private void attachTitles(List<RetrievedChunk> hits) {
        List<String> ids = hits.stream().map(RetrievedChunk::getTaiLieuId).distinct().toList();
        Map<String, String> titles = documentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Document::getTaiLieuId, Document::getTenHienThi, (a, b) -> a));
        hits.forEach(h -> h.setTenHienThi(titles.get(h.getTaiLieuId())));
    }

    private static long elapsed(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
