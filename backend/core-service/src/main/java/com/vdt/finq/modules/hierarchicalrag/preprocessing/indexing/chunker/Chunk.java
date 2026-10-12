package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// một chunk thuộc đúng một trang; page đếm từ 1, part từ 0
@Getter
@ToString
@AllArgsConstructor
public class Chunk {
    private final String documentId;
    private final int page;
    private final int part;
    private final String text;
}
