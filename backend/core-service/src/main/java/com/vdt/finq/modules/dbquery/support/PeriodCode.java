package com.vdt.finq.modules.dbquery.support;

// kỳ LLM được điền; Java đổi (năm, kỳ) ra ngày.
// Q1..Q4 riêng quý, H1/M9 luỹ kế 6/9 tháng, FY cả năm (chỉ tiêu thời điểm: số tại cuối kỳ)
public enum PeriodCode {
    Q1, Q2, Q3, Q4, H1, M9, FY
}
