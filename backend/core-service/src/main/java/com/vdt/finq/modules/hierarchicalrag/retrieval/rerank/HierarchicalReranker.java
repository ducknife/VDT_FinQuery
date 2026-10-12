package com.vdt.finq.modules.hierarchicalrag.retrieval.rerank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.vdt.finq.common.properties.RerankProperties;
import com.vdt.finq.common.properties.RetrievalProperties;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.embedder.Embedding;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievedChunk;
import com.vdt.finq.utils.TableToJson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// rerank phân cấp (paper Lee & Hong): 80 --0.6B--> 40 --4B--> 20.
// văn bản đưa reranker giống bản đã embed: tên tài liệu + chunk có bảng đổi JSON
@Component
@RequiredArgsConstructor
public class HierarchicalReranker {

    private final RerankClient client;
    private final RerankProperties rerankProperties;
    private final RetrievalProperties retrievalProperties;
    private final TableToJson tableToJson;

    @Getter
    @AllArgsConstructor
    public static class Outcome {
        private final List<RetrievedChunk> chunks;
        private final List<TierStat> tiers;
        // danh sách sau tầng 1 (bỏ qua tầng 1 thì là danh sách vào), để đo Recall@40
        private final List<RetrievedChunk> afterTier1;
    }

    public Outcome rerank(String query, List<RetrievedChunk> candidates) throws IOException, InterruptedException {
        List<TierStat> tiers = new ArrayList<>();
        List<RetrievedChunk> pool = candidates;

        String model1 = rerankProperties.getTier1Model();
        int keep1 = retrievalProperties.getTier1Keep();
        if (isBlank(model1)) {
            tiers.add(TierStat.skipped("tier1", "", pool.size(), "không cấu hình model tầng 1"));
        } else if (pool.size() <= keep1) {
            // ít hơn số cần giữ thì tầng 1 không loại được gì
            tiers.add(TierStat.skipped("tier1", model1, pool.size(), "chỉ có " + pool.size() + " ứng viên ≤ " + keep1));
        } else {
            pool = runTier("tier1", model1, query, pool, keep1, RetrievedChunk::setTier1Score,
                    RetrievedChunk::getTier1Score, tiers);
        }

        List<RetrievedChunk> afterTier1 = List.copyOf(pool);

        String model2 = rerankProperties.getTier2Model();
        int finalK = retrievalProperties.getFinalK();
        if (isBlank(model2)) {
            tiers.add(TierStat.skipped("tier2", "", pool.size(), "không cấu hình model tầng 2"));
            pool = pool.subList(0, Math.min(finalK, pool.size()));
        } else {
            pool = runTier("tier2", model2, query, pool, finalK, RetrievedChunk::setTier2Score,
                    RetrievedChunk::getTier2Score, tiers);
        }
        return new Outcome(List.copyOf(pool), List.copyOf(tiers), afterTier1);
    }

    // chấm điểm cả danh sách, sắp giảm dần, giữ keep chunk đầu (top N, không dùng ngưỡng điểm)
    private List<RetrievedChunk> runTier(String tier, String model, String query, List<RetrievedChunk> pool, int keep,
            BiConsumer<RetrievedChunk, Double> setScore, Function<RetrievedChunk, Double> getScore,
            List<TierStat> tiers) throws IOException, InterruptedException {
        long start = System.nanoTime();
        List<String> documents = pool.stream().map(this::documentText).toList();
        RerankClient.Scores scores = client.score(model, query, documents);
        for (int i = 0; i < pool.size(); i++) {
            setScore.accept(pool.get(i), scores.getScores()[i]);
        }
        // bằng điểm thì chunk có điểm vector cao hơn đứng trước, để lần nào chạy cũng ra một thứ tự
        List<RetrievedChunk> sorted = new ArrayList<>(pool);
        sorted.sort(Comparator.comparing(getScore, Comparator.reverseOrder())
                .thenComparing(RetrievedChunk::getVectorScore, Comparator.reverseOrder()));
        List<RetrievedChunk> kept = sorted.subList(0, Math.min(keep, sorted.size()));
        long millis = (System.nanoTime() - start) / 1_000_000;
        tiers.add(new TierStat(tier, model, pool.size(), kept.size(), millis, scores.getInputTokens(),
                scores.getCost(), null));
        return kept;
    }

    String documentText(RetrievedChunk chunk) {
        return Embedding.embedText(chunk.getTenHienThi(), tableToJson.convert(chunk.textForModel()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
