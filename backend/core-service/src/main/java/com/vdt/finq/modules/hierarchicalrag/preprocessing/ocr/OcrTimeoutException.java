package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import java.io.IOException;

// OCR Timeout Exception
public class OcrTimeoutException extends IOException {

    public OcrTimeoutException(String message) {
        super(message);
    }
}
