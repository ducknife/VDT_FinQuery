package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import java.io.IOException;
import java.util.Optional;

// nơi lưu kết quả OCR theo trang: kiểm, ghi, đọc, đánh dấu lỗi

public interface PageStore {
    boolean exists(String relativePath, int pageNumber) throws IOException; 
    void write(String relativePath, int pageNumber, String markdown) throws IOException;
    Optional<String> read(String relativePath, int pageNumber) throws IOException;
    void markFailed(String relativePath, int pageNumber, String reason) throws IOException;
}
