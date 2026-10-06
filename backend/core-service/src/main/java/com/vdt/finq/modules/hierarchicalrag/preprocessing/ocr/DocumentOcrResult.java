package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

// summary of one document run
public record DocumentOcrResult(int totalPages, int skipped, int succeeded, int failed) {

    // only true when nothing is left to do, used to decide documents.ngay_ocr
    public boolean complete() {
        return failed == 0;
    }
}
