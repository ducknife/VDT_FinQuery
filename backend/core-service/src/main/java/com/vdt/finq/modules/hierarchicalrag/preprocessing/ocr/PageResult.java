package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// kết quả của một trang, pageIndex đếm từ 0
@Getter
@ToString
@AllArgsConstructor
public class PageResult {

    private final int pageIndex;
    private final boolean success;
    private final int attempts;
    private final String error;

    public static PageResult ok(int pageIndex, int attempts) {
        return new PageResult(pageIndex, true, attempts, null);
    }

    public static PageResult failed(int pageIndex, int attempts, String error) {
        return new PageResult(pageIndex, false, attempts, error);
    }
}
