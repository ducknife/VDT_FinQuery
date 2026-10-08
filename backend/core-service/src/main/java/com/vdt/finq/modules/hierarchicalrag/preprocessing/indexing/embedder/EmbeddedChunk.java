package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.embedder;

import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.Chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// một chunk kèm vector của nó, sẵn sàng ghi vào Qdrant
@Getter
@ToString(exclude = "vector")
@AllArgsConstructor
public class EmbeddedChunk {

    private final Chunk chunk;

    // văn bản thực sự đã đem embed: tên tài liệu + nội dung chunk
    private final String embedText;

    private final float[] vector;
}
