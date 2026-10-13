package com.vdt.finq.modules.rag.retrieval;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// một chunk lấy từ Qdrant, mang theo điểm của từng bước để gỡ lỗi và đánh giá
@Getter
@Setter
@ToString(exclude = { "noiDung", "noiDungSach" })
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
    // markdown gốc, không đổi: hiển thị trang khi bấm trích dẫn
    private final String noiDung;
    // điểm cosine của Qdrant
    private final float vectorScore;
    // điền sau khi tìm: documents.ten_hien_thi
    private String tenHienThi;
    // điểm reranker (0..1), null nếu tầng đó không chạy với chunk này
    private Double tier1Score;
    private Double tier2Score;
    // bản đã bỏ đoạn tiếng Anh (EnglishFilter), làm MỘT lần sau khi tìm; null nếu không lọc
    private String noiDungSach;
    // văn bản đưa reranker và LLM, cũng là văn bản dùng để kiểm câu trích
    public String textForModel() {
        return noiDungSach != null ? noiDungSach : noiDung;
    }
}
