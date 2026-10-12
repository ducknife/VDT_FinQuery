package com.vdt.finq.modules.hierarchicalrag.retrieval.rerank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.properties.RerankProperties;
import com.vdt.finq.common.properties.RetrievalProperties;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievedChunk;
import com.vdt.finq.utils.TableToJson;

// rerank 2 tầng với reranker giả (không gọi API):  ./mvnw test -Dtest=HierarchicalRerankerTest
class HierarchicalRerankerTest {

    private static final Pattern RELEVANCE = Pattern.compile("liên quan=(\\d+)");
    private final ObjectMapper objectMapper = new ObjectMapper();

    // reranker giả: điểm = độ liên quan ghi trong chunk; model "small" cộng nhiễu, model "big" chính xác
    private class FakeRerankClient extends RerankClient {
        final List<String> calls = new ArrayList<>();

        FakeRerankClient(RerankProperties properties) {
            super(properties, objectMapper);
        }

        @Override
        public Scores score(String model, String query, List<String> documents) {
            calls.add(model + " x" + documents.size());
            double[] scores = new double[documents.size()];
            for (int i = 0; i < documents.size(); i++) {
                Matcher m = RELEVANCE.matcher(documents.get(i));
                double truth = m.find() ? Integer.parseInt(m.group(1)) / 100.0 : 0;
                double noise = model.equals("small") ? ((i * 37) % 11 - 5) / 100.0 : 0;
                scores[i] = Math.max(0, Math.min(1, truth + noise));
            }
            return new Scores(scores, documents.size() * 1000L, documents.size() * 0.00001);
        }
    }

    private RerankProperties rerankProperties(String tier1, String tier2) {
        RerankProperties p = new RerankProperties();
        p.setTier1Model(tier1);
        p.setTier2Model(tier2);
        return p;
    }

    private HierarchicalReranker reranker(FakeRerankClient client, RerankProperties p) {
        return new HierarchicalReranker(client, p, new RetrievalProperties(), new TableToJson(objectMapper));
    }

    // n chunk giả, độ liên quan khác nhau, điểm vector giảm dần theo thứ tự Qdrant trả về
    private static List<RetrievedChunk> candidates(int n) {
        List<RetrievedChunk> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int relevance = (i * 53) % 100;
            RetrievedChunk c = new RetrievedChunk("p" + i, "CTR-BCTC-HN-20240930-Q3", i + 1, 0, 2024, "Q3", "hop_nhat",
                    "bctc", "text", "trang " + (i + 1) + " liên quan=" + relevance, 0.9f - i * 0.001f);
            c.setTenHienThi("BCTC hợp nhất quý 3/2024");
            list.add(c);
        }
        return list;
    }

    private static void print(String title, HierarchicalReranker.Outcome outcome) {
        System.out.println("== " + title);
        outcome.getTiers().forEach(t -> System.out.printf("  %s %-6s vào %2d -> giữ %2d %s%n", t.getTier(), t.getModel(),
                t.getInputCount(), t.getKeptCount(), t.getSkipped() == null ? "" : "(bỏ qua: " + t.getSkipped() + ")"));
        System.out.println("  hạng | trang | vector | tầng 1 | tầng 2");
        for (int i = 0; i < Math.min(8, outcome.getChunks().size()); i++) {
            RetrievedChunk c = outcome.getChunks().get(i);
            System.out.printf("  %4d | %5d | %.3f  | %6s | %6s%n", i + 1, c.getTrang(), c.getVectorScore(),
                    c.getTier1Score() == null ? "-" : String.format("%.2f", c.getTier1Score()),
                    c.getTier2Score() == null ? "-" : String.format("%.2f", c.getTier2Score()));
        }
    }

    // luồng chính: 80 -> 40 (model nhỏ) -> 20 (model lớn), thứ tự cuối theo điểm model lớn
    @Test
    void eightyToFortyToTwenty() throws Exception {
        FakeRerankClient client = new FakeRerankClient(rerankProperties("small", "big"));
        HierarchicalReranker.Outcome outcome = reranker(client, rerankProperties("small", "big"))
                .rerank("doanh thu quý 3/2024", candidates(80));
        print("80 -> 40 -> 20", outcome);

        assertThat(client.calls).containsExactly("small x80", "big x40");
        assertThat(outcome.getChunks()).hasSize(20);
        assertThat(outcome.getTiers()).extracting(TierStat::getKeptCount).containsExactly(40, 20);
        // sắp giảm dần theo điểm tầng 2
        for (int i = 1; i < outcome.getChunks().size(); i++) {
            assertThat(outcome.getChunks().get(i - 1).getTier2Score())
                    .isGreaterThanOrEqualTo(outcome.getChunks().get(i).getTier2Score());
        }
        // chunk liên quan nhất (98, trang 67) phải lên đầu dù Qdrant xếp nó thứ 67/80
        assertThat(outcome.getChunks().get(0).getNoiDung()).contains("liên quan=98");
    }

    // bộ lọc chỉ ra 25 chunk (≤ 40): tầng 1 không loại được gì nên bỏ qua, chỉ chạy tầng 2
    @Test
    void fewCandidatesSkipTier1() throws Exception {
        FakeRerankClient client = new FakeRerankClient(rerankProperties("small", "big"));
        HierarchicalReranker.Outcome outcome = reranker(client, rerankProperties("small", "big"))
                .rerank("doanh thu", candidates(25));
        print("25 chunk: bỏ tầng 1", outcome);

        assertThat(client.calls).containsExactly("big x25");
        assertThat(outcome.getTiers().get(0).getSkipped()).contains("25");
        assertThat(outcome.getChunks()).hasSize(20);
    }

    // để trống model tầng 1 trong .env = rerank một tầng (cấu hình C/D của benchmark)
    @Test
    void singleTierWhenTier1Blank() throws Exception {
        FakeRerankClient client = new FakeRerankClient(rerankProperties("", "big"));
        HierarchicalReranker.Outcome outcome = reranker(client, rerankProperties("", "big"))
                .rerank("doanh thu", candidates(80));
        print("một tầng (model lớn chấm cả 80)", outcome);
        assertThat(client.calls).containsExactly("big x80");
    }

    // văn bản đưa reranker = [tên tài liệu] + chunk có bảng đổi sang JSON (đúng bản đã embed)
    @Test
    void rerankTextIsWhatWasEmbedded() {
        HierarchicalReranker reranker = reranker(new FakeRerankClient(new RerankProperties()), new RerankProperties());
        RetrievedChunk chunk = new RetrievedChunk("p", "id", 7, 0, 2024, "Q3", "hop_nhat", "bctc", "table",
                "| Chỉ tiêu | Mã số | Năm nay |\n|---|---|---|\n| Doanh thu thuần | 10 | 8.195.000 |", 0.8f);
        chunk.setTenHienThi("BCTC hợp nhất quý 3/2024");
        String text = reranker.documentText(chunk);
        System.out.println("== văn bản đưa reranker:\n" + text);
        assertThat(text).startsWith("[BCTC hợp nhất quý 3/2024]").contains("\"Năm nay\":\"8.195.000\"");
    }

    // response thật của DeepInfra (gọi thử 11/10/2026); số điểm lệch số chunk thì báo lỗi
    @Test
    void parsesDeepInfraResponse() throws IOException {
        RerankClient client = new RerankClient(new RerankProperties(), objectMapper);
        String json = "{\"request_id\":\"x\",\"inference_status\":{\"runtime_ms\":86,\"cost\":2.93e-06,"
                + "\"tokens_input\":293},\"scores\":[0.9999,4.79e-05,7.5e-06],\"input_tokens\":293}";
        RerankClient.Scores scores = client.parse(json, 3);
        assertThat(scores.getScores()).containsExactly(0.9999, 4.79e-05, 7.5e-06);
        assertThat(scores.getInputTokens()).isEqualTo(293);
        assertThat(scores.getCost()).isEqualTo(2.93e-06);
        assertThatThrownBy(() -> client.parse(json, 4)).hasMessageContaining("cần 4");
    }
}
