package com.vdt.finq.config.testrunner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.vdt.finq.modules.agent.AskResult;
import com.vdt.finq.modules.agent.loop.QueryLoop;
import com.vdt.finq.modules.evaluation.baseline.BaselineRunner;
import com.vdt.finq.modules.rag.generation.Citation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// hỏi thật một câu lúc app khởi động, ghi nguyên văn câu trả lời ra trial-answer.txt (mặc định câu X06);
// bật bằng answer.trial.enabled=true (tuỳ chọn: answer.trial.question, answer.trial.configs)
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "answer.trial.enabled", havingValue = "true")
public class AnswerTrialRunner implements CommandLineRunner {

    private static final Path OUT = Path.of("trial-answer.txt");

    private final QueryLoop queryLoop;
    private final BaselineRunner baselines;

    @Value("${answer.trial.question:Doanh thu thuần hợp nhất cả năm 2026 là bao nhiêu?}")
    private String question;
    @Value("${answer.trial.configs:FINQUERY,TEXT_TO_SQL,LLM_ONLY}")
    private List<String> configs;

    @Override
    public void run(String... args) throws Exception {
        StringBuilder out = new StringBuilder("CAU HOI: ").append(question).append("\n\n");
        for (String c : configs) {
            out.append("=== ").append(c).append('\n');
            if (c.equals("LLM_ONLY")) {
                BaselineRunner.PlainAnswer p = baselines.llmOnly(question);
                out.append(p.answer()).append("\n\n");
                out.append("thoi gian: ").append(p.millis()).append(" ms, usd: ").append(p.cost()).append("\n\n");
                continue;
            }
            AskResult r = c.equals("FINQUERY") ? queryLoop.ask(question) : baselines.textToSql(question);
            out.append("status: ").append(r.getStatus()).append('\n');
            out.append(r.getAnswer()).append("\n\n");
            if (!r.getMissing().isBlank()) {
                out.append("missing: ").append(r.getMissing()).append('\n');
            }
            for (Citation ct : r.getCitations()) {
                out.append("trich dan ").append(ct.getId()).append(ct.isVerified() ? " (dung)" : " (KHONG thay trong nguon)")
                        .append(": ").append(ct.getQuote()).append('\n');
            }
            for (String t : r.getToolTrace()) {
                out.append("tool: ").append(t).append('\n');
            }
            out.append("thoi gian: ").append(r.getTimingsMs()).append(", usd: ").append(r.getCostUsd().get("total"))
                    .append("\n\n");
        }
        Files.writeString(OUT, out, StandardCharsets.UTF_8);
        log.info("trial câu trả lời: đã ghi {}", OUT.toAbsolutePath());
    }
}
