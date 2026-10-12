package com.vdt.finq.modules.agent.tools.financial.support;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// phạm vi báo cáo; value là đúng chuỗi trong cột financial_figures.pham_vi
@Getter
@RequiredArgsConstructor
public enum Scope {
    CONSOLIDATED("hop_nhat", "hợp nhất"),
    SEPARATE("rieng", "riêng");

    private final String value;
    private final String label;
}
