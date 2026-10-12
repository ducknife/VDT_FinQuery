package com.vdt.finq.modules.hierarchicalrag.retrieval;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

// toạ độ gọn của một chunk (không kèm nội dung), để ghi thứ hạng ở từng tầng cho đánh giá
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class ChunkRef {
    private final String taiLieuId;
    private final int trang;
    private final int phan;

    public static ChunkRef of(RetrievedChunk chunk) {
        return new ChunkRef(chunk.getTaiLieuId(), chunk.getTrang(), chunk.getPhan());
    }
}
