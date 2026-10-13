package com.vdt.finq.modules.rag.preprocessing.indexing;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// kết quả một lượt nạp nhiều tài liệu
@Getter
@ToString
@AllArgsConstructor
public class IndexingSummary {
    private final int total;
    private final int succeeded;
    private final int chunks;
    // tai_lieu_id lỗi; ngay_embed của chúng vẫn NULL nên lần chạy sau tự làm lại
    private final List<String> failed;
}
