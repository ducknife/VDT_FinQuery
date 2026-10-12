package com.vdt.finq.modules.agent.evidence;

import lombok.AllArgsConstructor;
import lombok.Getter;

// một bằng chứng: chunk tài liệu (C#) hoặc số liệu DB (D#); mã do Java gán nên LLM không bịa được nguồn
@Getter
@AllArgsConstructor
public class Evidence {

    public enum Kind {
        CHUNK, FIGURE
    }

    private final String id;
    private final Kind kind;
    private final String taiLieuId;
    private final String tenHienThi;
    private final int trang;
    private final int phan;
    // nội dung đưa cho LLM ở bước trả lời (chunk: markdown gốc)
    private final String content;
    // điểm rerank cuối (chunk), để xếp thứ tự; số liệu DB thì null
    private final Double score;

    public String source() {
        if (taiLieuId == null) {
            return "Java tính từ các số liệu đã dẫn";
        }
        return (tenHienThi == null ? taiLieuId : tenHienThi) + ", trang " + trang;
    }
}
