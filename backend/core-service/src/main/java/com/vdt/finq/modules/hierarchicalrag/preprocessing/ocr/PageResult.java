package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

// outcome of one page, pageIndex is zero based
public record PageResult(int pageIndex, boolean success, int attempts, String error) {

    public static PageResult ok(int pageIndex, int attempts) {
        return new PageResult(pageIndex, true, attempts, null);
    }

    public static PageResult failed(int pageIndex, int attempts, String error) {
        return new PageResult(pageIndex, false, attempts, error);
    }
}
