package com.vdt.finq.modules.financial;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// kết quả một lượt trích số trên cả kho
@Getter
@ToString
@AllArgsConstructor
public class ExtractionSummary {
    private final int documents;
    private final int tables;
    // số đọc được và qua hết các chốt, trước khi chọn nguồn
    private final int candidates;
    // dòng ghi vào financial_figures (kể cả số gốc bị thay)
    private final int figures;
    private final int restated;
    // cùng kỳ, các nguồn không phải trình bày lại mà khác số: để soát
    private final int conflicts;
    private final Map<String, Integer> issuesByType;
    private final List<String> skippedDocuments;
}
