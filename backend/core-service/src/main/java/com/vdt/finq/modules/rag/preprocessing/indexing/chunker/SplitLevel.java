package com.vdt.finq.modules.rag.preprocessing.indexing.chunker;

// các mức cắt của Chunking, từ thô đến mịn; thứ tự khai báo chính là thứ tự đệ quy
enum SplitLevel {
    HEADING, // tách trước mỗi dòng heading markdown
    BLOCK,   // tách ở dòng trống và chỗ chuyển giữa bảng với chữ
    LINE,    // tách theo dòng (bảng lặp lại tiêu đề cột) hoặc theo câu
    ATOM;    // câu hoặc dòng bảng: không chia được nữa

    // mức mịn hơn kế tiếp; ATOM là mức cuối nên trả lại chính nó
    SplitLevel next() {
        SplitLevel[] levels = values();
        return levels[Math.min(ordinal() + 1, levels.length - 1)];
    }
}
