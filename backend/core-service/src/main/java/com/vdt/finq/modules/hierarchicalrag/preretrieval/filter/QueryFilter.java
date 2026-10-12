package com.vdt.finq.modules.hierarchicalrag.preretrieval.filter;

import java.util.List;

import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.document.DocumentEnums.PhamVi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// điều kiện lọc payload Qdrant; danh sách rỗng là không lọc; tài liệu không có ky/pham_vi vẫn giữ
@Getter
@ToString
@AllArgsConstructor
public class QueryFilter {
    // luôn có: mặc định CTR
    private final String maCongTy;
    // năm người hỏi nói tới
    private final List<Integer> nams;
    // năm được nhận cho báo cáo định kỳ (có kỳ) và cho văn bản sự kiện (không có kỳ); xem FilterExtractor
    private final List<Integer> periodNams;
    private final List<Integer> eventNams;
    private final List<Ky> kys;
    // luôn có: mặc định hợp nhất (quy tắc 6 của bộ chuẩn hoá)
    private final PhamVi phamVi;
    private final List<LoaiTaiLieu> loais;
    // luật nào đã chạy, ví dụ "nam:2024", "ky:Q3", "loai:bctn"
    private final List<String> applied;
}
