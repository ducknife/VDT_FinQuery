package com.vdt.finq.modules.rag.generation;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

// câu trả lời đã kiểm: trạng thái, nội dung, trích dẫn, các số không tìm thấy trong nguồn
@Getter
@AllArgsConstructor
public class AnswerResult {
    // answered | partial | not_found | out_of_scope
    private final String status;
    private final String answer;
    private final List<Citation> citations;
    private final String missing;
    // số trong câu trả lời không có trong bằng chứng nào (cảnh báo bịa số)
    private final List<String> unverifiedNumbers;
    // mã được nhắc trong câu trả lời mà không có trong kho bằng chứng
    private final List<String> unknownIds;
    // "single" | "split" (vượt ngưỡng ngữ cảnh, chia 2 nửa rồi hợp nhất theo paper)
    private final String mode;
    private final int promptTokens;
    private final int completionTokens;
    private final double cost;
    private final long millis;
    // phép tính model khai báo, Java đã kiểm và tính ({{K#}} trong answer đã được điền)
    private final List<EvidenceCalculator.Calculation> calculations;
}
