package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import java.io.IOException;

// lỗi hết giờ khi OCR: stream im lặng quá lâu hoặc trả về rỗng
public class OcrTimeoutException extends IOException {

    public OcrTimeoutException(String message) {
        super(message);
    }
}
