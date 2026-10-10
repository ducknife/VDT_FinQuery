package com.vdt.finq.modules.financial.extraction;

import com.vdt.finq.modules.financial.FinancialEnums.MauBieu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// một con số đọc được từ một ô của báo cáo, chưa qua bước chọn nguồn có thẩm quyền
@Getter
@ToString
@AllArgsConstructor
public class FigureCandidate {
    private final String maNoiBo;
    private final Period period;
    private final String phamVi;
    private final long giaTri;

    // dẫn chứng: tài liệu nào, trang mấy
    private final String taiLieuId;
    private final int trang;

    private final MauBieu mauBieu;
    private final String ma;
    // kỳ của cột chứa số (khác period ở tiền đầu kỳ, tiền cuối kỳ của B03)
    private final Period columnPeriod;
    private final boolean current;
    private final boolean restated;
}
