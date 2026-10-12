package com.vdt.finq.modules.hierarchicalrag.retrieval;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// một chunk lấy từ Qdrant, mang theo điểm của từng bước để gỡ lỗi và đánh giá
@Getter
@Setter
@ToString(exclude = "noiDung")
@RequiredArgsConstructor
public class RetrievedChunk {
    private final String pointId;
    private final String taiLieuId;
    private final int trang;
    private final int phan;
    private final Integer nam;
    private final String ky;
    private final String phamVi;
    private final String loai;
    private final String elementType;
    // markdown gốc, dùng để trích dẫn và đưa LLM
    private final String noiDung;
    // điểm cosine của Qdrant
    private final float vectorScore;
    // điền sau khi tìm: documents.ten_hien_thi
    private String tenHienThi;
    // điểm reranker (0..1), null nếu tầng đó không chạy với chunk này
    private Double tier1Score;
    private Double tier2Score;
}
