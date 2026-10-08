package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.strategy;

import java.io.IOException;
import java.nio.file.Path;

import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.DocumentOcrResult;

// mỗi định dạng file một implementation, muốn thêm định dạng thì thêm một @Component
public interface DocumentExtractor {

    // phải rẻ, không mở file ở đây
    boolean supports(String fileName);

    // mọi implementation đều ghi qua PageStore, nên phần sau của luồng
    // chỉ thấy các file .md đánh số trang, dù định dạng gốc là gì
    DocumentOcrResult extract(Path file, String relativePath) throws IOException;
}
