package com.vdt.finq.modules.rag.preretrieval;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vdt.finq.modules.document.DocumentRepository;
import com.vdt.finq.modules.rag.preretrieval.filter.FilterExtractor;
import com.vdt.finq.modules.rag.preretrieval.keyword.KeywordExtractor;
import com.vdt.finq.modules.rag.preretrieval.normalizer.FinancialTextNormalizer;
import com.vdt.finq.modules.rag.preretrieval.normalizer.NormalizedQuery;

import lombok.RequiredArgsConstructor;

// tiền truy xuất phía câu hỏi: chuẩn hoá -> trích từ khoá -> trích bộ lọc
@Component
@RequiredArgsConstructor
public class QueryPreprocessor {

    private final FinancialTextNormalizer normalizer;
    private final KeywordExtractor keywordExtractor;
    private final FilterExtractor filterExtractor;
    private final DocumentRepository documentRepository;

    // useKeywords=false để đo riêng tác dụng của từ khoá trong benchmark
    public PreparedQuery prepare(String question, boolean useKeywords) {
        // "năm nay", "quý trước" tính theo kỳ mới nhất trong kho, không theo đồng hồ hệ thống
        LocalDate latest = documentRepository.findLatestPeriodEnd();
        NormalizedQuery normalized = normalizer.normalizeQuery(question, latest);
        List<String> keywords = useKeywords ? keywordExtractor.extract(normalized) : List.of();
        return new PreparedQuery(normalized, keywords, compose(normalized.getText(), keywords),
                filterExtractor.extract(normalized, latest == null ? null : latest.getYear()));
    }

    static String compose(String normalizedText, List<String> keywords) {
        if (keywords.isEmpty()) {
            return normalizedText;
        }
        return normalizedText + "\nTừ khoá: " + String.join("; ", keywords);
    }
}
