package com.vdt.finq.modules.rag.retrieval;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// công tắc từng bước, để benchmark đo riêng tác dụng của mỗi bước (bật hết = cấu hình chính)
@Getter
@ToString
@AllArgsConstructor
public class RetrievalOptions {
    public static final RetrievalOptions DEFAULT = new RetrievalOptions(true, true, true, true);
    // RAG cơ bản để đối chứng: embed nguyên câu hỏi, không lọc, không rerank, lấy top k
    public static final RetrievalOptions BASIC_RAG = new RetrievalOptions(false, false, false, false);

    private final boolean keywords;
    private final boolean filter;
    private final boolean rerank;
    // false: bỏ cả chuẩn hoá, embed và rerank bằng nguyên câu người dùng gõ
    private final boolean normalize;

    public RetrievalOptions(boolean keywords, boolean filter, boolean rerank) {
        this(keywords, filter, rerank, true);
    }
}
