package com.vdt.finq.modules.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.vdt.finq.modules.hierarchicalrag.retrieval.ChunkRef;

// chỉ số đánh giá truy hồi, tính theo TRANG (nhãn của bộ câu hỏi ghi theo trang):
//   lấy k chunk đầu -> đổi về trang, mỗi trang tính một lần ở vị trí xuất hiện đầu tiên -> so với trang vàng
// gold: "taiLieuId#trang" -> mức liên quan (2 = trang chính, 1 = trang phụ)
public final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    public static String pageKey(String taiLieuId, int trang) {
        return taiLieuId + "#" + trang;
    }

    // k chunk đầu -> danh sách trang không trùng, giữ thứ tự
    public static List<String> topPages(List<ChunkRef> chunks, int k) {
        Set<String> pages = new LinkedHashSet<>();
        for (int i = 0; i < Math.min(k, chunks.size()); i++) {
            pages.add(pageKey(chunks.get(i).getTaiLieuId(), chunks.get(i).getTrang()));
        }
        return new ArrayList<>(pages);
    }

    // có ít nhất một trang vàng trong danh sách không
    public static boolean hit(List<String> pages, Map<String, Integer> gold) {
        return pages.stream().anyMatch(gold::containsKey);
    }

    // câu cần nhiều bằng chứng: mọi bằng chứng đều có ít nhất một trang trong danh sách
    // groupsOfPage: trang vàng -> các bằng chứng mà trang đó chứa
    public static boolean allEvidence(List<String> pages, Map<String, Set<Integer>> groupsOfPage, int evidenceCount) {
        Set<Integer> covered = new HashSet<>();
        for (String page : pages) {
            covered.addAll(groupsOfPage.getOrDefault(page, Set.of()));
        }
        return covered.size() >= evidenceCount;
    }

    // nDCG: điểm = mức liên quan / log2(hạng + 1), chia cho điểm của thứ tự lý tưởng
    public static double ndcg(List<String> pages, Map<String, Integer> gold, int k) {
        double dcg = 0;
        for (int i = 0; i < Math.min(k, pages.size()); i++) {
            dcg += gold.getOrDefault(pages.get(i), 0) / log2(i + 2);
        }
        int[] ideal = gold.values().stream().mapToInt(Integer::intValue).sorted().toArray();
        double idcg = 0;
        for (int i = 0; i < Math.min(k, ideal.length); i++) {
            idcg += ideal[ideal.length - 1 - i] / log2(i + 2);
        }
        return idcg == 0 ? 0 : dcg / idcg;
    }

    // nDCG theo bằng chứng: mỗi bằng chứng chỉ cần MỘT trang (cùng con số in lại ở nhiều báo cáo thì lấy
    // một trang là đủ trả lời). điểm của trang = mức của nó × số bằng chứng MỚI nó phủ được, chia log2(hạng+1);
    // lý tưởng = mỗi bằng chứng một trang mức 2 ở các hạng đầu. trần 1 (một trang phủ nhiều bằng chứng)
    public static double ndcgByEvidence(List<String> pages, Map<String, Integer> gold,
            Map<String, Set<Integer>> groupsOfPage, int evidenceCount, int k) {
        if (evidenceCount == 0) {
            return 0;
        }
        Set<Integer> covered = new HashSet<>();
        double dcg = 0;
        for (int i = 0; i < Math.min(k, pages.size()); i++) {
            Set<Integer> fresh = new HashSet<>(groupsOfPage.getOrDefault(pages.get(i), Set.of()));
            fresh.removeAll(covered);
            if (!fresh.isEmpty()) {
                dcg += gold.getOrDefault(pages.get(i), 0) * fresh.size() / log2(i + 2);
                covered.addAll(fresh);
            }
        }
        double idcg = 0;
        for (int i = 0; i < Math.min(k, evidenceCount); i++) {
            idcg += 2 / log2(i + 2);
        }
        return Math.min(1.0, dcg / idcg);
    }

    // 1 / hạng của trang vàng đầu tiên; không có thì 0
    public static double reciprocalRank(List<String> pages, Map<String, Integer> gold) {
        for (int i = 0; i < pages.size(); i++) {
            if (gold.containsKey(pages.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0;
    }

    // hạng (từ 1) của trang vàng đầu tiên; không có thì 0
    public static int firstGoldRank(List<String> pages, Map<String, Integer> gold) {
        for (int i = 0; i < pages.size(); i++) {
            if (gold.containsKey(pages.get(i))) {
                return i + 1;
            }
        }
        return 0;
    }

    // bị lừa: có trang dễ nhầm xếp TRÊN trang vàng đầu tiên (hoặc có trang dễ nhầm mà không có trang vàng)
    public static boolean fooled(List<String> pages, Map<String, Integer> gold, Set<String> traps) {
        for (String page : pages) {
            if (gold.containsKey(page)) {
                return false;
            }
            if (traps.contains(page)) {
                return true;
            }
        }
        return false;
    }

    // bootstrap ghép cặp: diffs[i] = chỉ số cấu hình B - cấu hình A trên cùng câu i.
    // trả {trung bình, cận dưới 2,5%, cận trên 97,5%}; khoảng không chứa 0 thì chênh lệch đáng tin
    public static double[] bootstrapMeanCi(double[] diffs, int resamples, long seed) {
        if (diffs.length == 0) {
            return new double[] { 0, 0, 0 };
        }
        Random random = new Random(seed);
        double[] means = new double[resamples];
        for (int r = 0; r < resamples; r++) {
            double sum = 0;
            for (int i = 0; i < diffs.length; i++) {
                sum += diffs[random.nextInt(diffs.length)];
            }
            means[r] = sum / diffs.length;
        }
        Arrays.sort(means);
        double mean = Arrays.stream(diffs).average().orElse(0);
        return new double[] { mean, means[(int) (resamples * 0.025)], means[(int) (resamples * 0.975) - 1] };
    }

    // phân vị p (0..100) của danh sách số, nội suy gần nhất
    public static long percentile(List<Long> values, double p) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Long> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
