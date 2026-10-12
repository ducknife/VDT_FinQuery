package com.vdt.finq.modules.agent;

import java.util.List;
import java.util.Map;

import com.vdt.finq.modules.hierarchicalrag.generation.Citation;

import lombok.AllArgsConstructor;
import lombok.Getter;

// kết quả trả cho người dùng cho một câu hỏi, kèm số đo để đánh giá và gỡ lỗi
@Getter
@AllArgsConstructor
public class AskResult {
    private final String question;
    // answered | partial | not_found | out_of_scope
    private final String status;
    private final String answer;
    private final List<Citation> citations;
    private final String missing;
    // cảnh báo kỷ luật bằng chứng: số không có trong nguồn, mã trích dẫn không tồn tại
    private final List<String> unverifiedNumbers;
    private final List<String> unknownIds;
    // số lượt chọn tool đã dùng và các lần gọi tool
    private final int toolRounds;
    private final List<String> toolTrace;
    // thời gian (ms): agent (các lượt chọn tool), tool, answer, total
    private final Map<String, Long> timingsMs;
    // tiền thật (USD): agent, rerank, answer, total
    private final Map<String, Double> costUsd;
    private final int promptTokens;
    private final int completionTokens;
    private final String promptVersion;
    private final String model;
}
