package com.vdt.finq.modules.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vdt.finq.modules.agent.evidence.Evidence;
import com.vdt.finq.modules.agent.tools.QueryState;
import com.vdt.finq.modules.agent.tools.SearchDocuments;
import com.vdt.finq.modules.dbquery.GetFinancialFigures;
import com.vdt.finq.modules.dbquery.support.PeriodCode;
import com.vdt.finq.modules.dbquery.support.PeriodInput;
import com.vdt.finq.modules.rag.generation.AnswerGenerator;
import com.vdt.finq.modules.rag.generation.AnswerResult;
import com.vdt.finq.modules.rag.preretrieval.QueryPreprocessor;

// phát lại bước trả lời với bằng chứng cố định (bỏ biến động của agent) để đo tỷ lệ đọc sót:
//     ./mvnw test "-Dtest=AnswerReplayTest" "-Dreplay=true" "-Dreplay.q=<câu hỏi>" "-Dreplay.search=<truy vấn agent>"
//         "-Dreplay.figures=VON_GOP_CHU_SO_HUU@2025FY;VON_GOP_CHU_SO_HUU@2026H1" "-Dreplay.expect=<chuỗi đáp án>" "-Dreplay.n=3"
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "answer.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "replay", matches = "true")
class AnswerReplayTest {

    @Autowired
    private SearchDocuments searchDocuments;
    @Autowired
    private GetFinancialFigures getFinancialFigures;
    @Autowired
    private AnswerGenerator answerGenerator;
    @Autowired
    private QueryPreprocessor preprocessor;

    @Test
    void replay() throws Exception {
        String question = prop("replay.q");
        String expect = System.getProperty("replay.expect", "");
        int n = Integer.parseInt(System.getProperty("replay.n", "3"));

        QueryState state = new QueryState();
        ToolContext ctx = new ToolContext(Map.of(QueryState.KEY, state));
        searchDocuments.searchDocuments(prop("replay.search"), ctx);
        for (String f : System.getProperty("replay.figures", "").split(";")) {
            if (f.isBlank()) {
                continue;
            }
            String[] p = f.strip().split("@");
            PeriodInput period = new PeriodInput(Integer.parseInt(p[1].substring(0, 4)), PeriodCode.valueOf(p[1].substring(4)));
            getFinancialFigures.getFinancialFigures(List.of(p[0]), List.of(period), null, ctx);
        }
        List<Evidence> evidence = state.getEvidence().all();
        System.out.println("[REPLAY] bằng chứng: " + evidence.stream()
                .map(e -> e.getId() + "=" + (e.getTaiLieuId() == null ? "tính" : e.getTaiLieuId() + "#" + e.getTrang())).toList());

        String normalized = preprocessor.prepare(question, false).getNormalized().getText();
        List<String> verdicts = new ArrayList<>();
        double cost = 0;
        for (int i = 1; i <= n; i++) {
            AnswerResult r = answerGenerator.generate(question, normalized, evidence);
            boolean hit = !expect.isEmpty() && r.getAnswer().contains(expect);
            verdicts.add(hit ? "đúng" : "sót");
            cost += r.getCost();
            System.out.println("[REPLAY] lần " + i + ": " + (hit ? "ĐÚNG" : "SÓT") + " | " + r.getStatus() + " | trích "
                    + r.getCitations().stream().map(c -> c.getId()).toList() + "\n" + r.getAnswer() + "\n");
        }
        System.out.printf("[REPLAY] TỔNG: %s, %.4f USD%n", verdicts, cost);
    }

    // tham số -D qua mvnw trên Windows mất dấu tiếng Việt (cp1252): "@<file>" thì đọc chuỗi từ file UTF-8
    private static String prop(String key) {
        String v = System.getProperty(key);
        try {
            return v != null && v.startsWith("@")
                    ? java.nio.file.Files.readString(java.nio.file.Path.of(v.substring(1)), java.nio.charset.StandardCharsets.UTF_8).strip()
                    : v;
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
