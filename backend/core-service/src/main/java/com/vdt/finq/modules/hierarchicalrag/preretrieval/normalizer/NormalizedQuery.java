package com.vdt.finq.modules.hierarchicalrag.preretrieval.normalizer;

import java.util.List;

import com.vdt.finq.modules.document.DocumentEnums.PhamVi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// câu hỏi sau chuẩn hoá. text dùng để TÌM (embed, rerank, trích bộ lọc);
// original giữ nguyên câu người dùng gõ, để hiện lại và đưa cho LLM khi viết câu trả lời
@Getter
@ToString
@AllArgsConstructor
public class NormalizedQuery {
    private final String original;
    private final String text;
    // số tiền trong câu hỏi, đã đổi ra đồng (cho nhánh DB và chốt kiểm "số phải có nguồn")
    private final List<Long> amounts;
    // ngữ cảnh điền từ metadata (quy tắc 6)
    private final String maCongTy;
    private final boolean maCongTyMacDinh;
    private final PhamVi phamVi;
    private final boolean phamViMacDinh;
    // quy tắc nào đã chạy, để gỡ lỗi và viết báo cáo, ví dụ "abbreviation:LNST", "period:Q3/2024"
    private final List<String> applied;
}
