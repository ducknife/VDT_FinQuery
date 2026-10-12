package com.vdt.finq.modules.hierarchicalrag.retrieval.rerank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// số đo của một tầng rerank, dùng cho log, benchmark và bảng chi phí trong báo cáo
@Getter
@ToString
@AllArgsConstructor
public class TierStat {
    // "tier1" | "tier2"
    private final String tier;
    private final String model;
    private final int inputCount;
    private final int keptCount;
    private final long millis;
    private final long inputTokens;
    private final double cost;
    // null nếu tầng có chạy; ngược lại là lý do bỏ qua
    private final String skipped;

    static TierStat skipped(String tier, String model, int count, String reason) {
        return new TierStat(tier, model, count, count, 0, 0, 0, reason);
    }
}
