package com.vdt.finq.modules.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vdt.finq.modules.hierarchicalrag.retrieval.ChunkRef;

// kiểm các chỉ số trên đúng ví dụ đã giải thích: trang vàng P (mức 2), Q (mức 2), R (mức 1)
class RetrievalMetricsTest {

    private static final Map<String, Integer> GOLD = Map.of("P", 2, "Q", 2, "R", 1);

    @Test
    void ndcgMatchesWorkedExample() {
        // X, P, R, Y, Q -> 2,54 / 3,76 = 0,67
        assertThat(RetrievalMetrics.ndcg(List.of("X", "P", "R", "Y", "Q"), GOLD, 20)).isCloseTo(0.674, within(0.001));
        // thứ tự lý tưởng -> 1; P và Q cùng mức nên đổi chỗ vẫn 1
        assertThat(RetrievalMetrics.ndcg(List.of("P", "Q", "R"), GOLD, 20)).isCloseTo(1.0, within(1e-9));
        assertThat(RetrievalMetrics.ndcg(List.of("Q", "P", "R"), GOLD, 20)).isCloseTo(1.0, within(1e-9));
        // trang phụ đứng trước trang chính -> 0,87
        assertThat(RetrievalMetrics.ndcg(List.of("R", "P", "Q"), GOLD, 20)).isCloseTo(0.867, within(0.001));
        // không có trang vàng nào -> 0
        assertThat(RetrievalMetrics.ndcg(List.of("X", "Y"), GOLD, 20)).isZero();
        // trang vàng nằm ngoài top k thì không tính
        assertThat(RetrievalMetrics.ndcg(List.of("X", "Y", "P"), GOLD, 2)).isZero();
    }

    // hai chunk cùng một trang chỉ tính một lần, ở vị trí đầu tiên
    @Test
    void chunksOfSamePageCountOnce() {
        List<ChunkRef> chunks = List.of(new ChunkRef("A", 5, 0), new ChunkRef("A", 5, 1), new ChunkRef("B", 2, 0));
        assertThat(RetrievalMetrics.topPages(chunks, 20)).containsExactly("A#5", "B#2");
        assertThat(RetrievalMetrics.topPages(chunks, 1)).containsExactly("A#5");
    }

    @Test
    void recallRankAndFooled() {
        List<String> pages = List.of("X", "TRAP", "P");
        assertThat(RetrievalMetrics.hit(pages, GOLD)).isTrue();
        assertThat(RetrievalMetrics.reciprocalRank(pages, GOLD)).isCloseTo(1.0 / 3, within(1e-9));
        assertThat(RetrievalMetrics.firstGoldRank(pages, GOLD)).isEqualTo(3);
        assertThat(RetrievalMetrics.fooled(pages, GOLD, Set.of("TRAP"))).isTrue();
        assertThat(RetrievalMetrics.fooled(List.of("P", "TRAP"), GOLD, Set.of("TRAP"))).isFalse();
    }

    // câu cần 2 bằng chứng: thiếu một bằng chứng là chưa đủ
    @Test
    void allEvidenceNeedsEveryGroup() {
        Map<String, Set<Integer>> groups = Map.of("P", Set.of(0), "Q", Set.of(1));
        assertThat(RetrievalMetrics.allEvidence(List.of("P", "X"), groups, 2)).isFalse();
        assertThat(RetrievalMetrics.allEvidence(List.of("Q", "P"), groups, 2)).isTrue();
    }

    // một con số in lại ở 3 báo cáo (A, B, C cùng mức 2, cùng bằng chứng 0): lấy được một trang ở hạng 1 là đủ
    @Test
    void ndcgByEvidenceNeedsOnePagePerEvidence() {
        Map<String, Integer> gold = Map.of("A", 2, "B", 2, "C", 2);
        Map<String, Set<Integer>> groups = Map.of("A", Set.of(0), "B", Set.of(0), "C", Set.of(0));
        assertThat(RetrievalMetrics.ndcgByEvidence(List.of("A", "X"), gold, groups, 1, 20)).isCloseTo(1.0, within(1e-9));
        // nDCG đầy đủ phạt vì thiếu B, C
        assertThat(RetrievalMetrics.ndcg(List.of("A", "X"), gold, 20)).isLessThan(0.5);
        // trang đúng ở hạng 2 -> 1/log2(3) = 0,63
        assertThat(RetrievalMetrics.ndcgByEvidence(List.of("X", "B"), gold, groups, 1, 20)).isCloseTo(0.631, within(0.001));
        // câu 2 bằng chứng, chỉ có bằng chứng 0 ở hạng 1 -> 2 / (2 + 1,26) = 0,61
        Map<String, Set<Integer>> two = Map.of("A", Set.of(0), "B", Set.of(1));
        assertThat(RetrievalMetrics.ndcgByEvidence(List.of("A", "X"), Map.of("A", 2, "B", 2), two, 2, 20))
                .isCloseTo(0.613, within(0.001));
    }

    @Test
    void bootstrapIsDeterministicAndBracketsMean() {
        double[] diffs = { 0.1, 0.2, 0.0, 0.3, 0.1, 0.2, 0.1, 0.0 };
        double[] ci = RetrievalMetrics.bootstrapMeanCi(diffs, 10_000, 42);
        assertThat(ci[0]).isCloseTo(0.125, within(1e-9));
        assertThat(ci[1]).isLessThan(ci[0]).isGreaterThan(0);
        assertThat(ci[2]).isGreaterThan(ci[0]);
        assertThat(RetrievalMetrics.bootstrapMeanCi(diffs, 10_000, 42)).containsExactly(ci);
    }
}
