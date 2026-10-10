package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// một chunk: luôn thuộc đúng một trang của một tài liệu
// page đếm từ 1, part đếm từ 0 (trang không bị cắt chỉ có part 0)
@Getter
@ToString
@AllArgsConstructor
public class Chunk {
    private final String documentId;
    private final int page;
    private final int part;
    private final String text;
}
