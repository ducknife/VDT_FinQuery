package com.vdt.finq.modules.rag.preprocessing.indexing.embedder;

import com.vdt.finq.modules.rag.preprocessing.indexing.chunker.Chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// một chunk kèm vector của nó, sẵn sàng ghi vào Qdrant
@Getter
@ToString(exclude = "vector")
@AllArgsConstructor
public class EmbeddedChunk {
    private final Chunk chunk;
    private final String embedText;
    private final float[] vector;
}
