package com.vdt.finq.modules.financial.extraction;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

// một bảng BCTC đã nhận ra: các dòng (mã, tên, giá trị); giá trị là các ô cuối mỗi dòng
@Getter
@AllArgsConstructor
public class StatementTable {
    // các dòng tiêu đề (trước dòng kẻ và các dòng chỉ ghi kỳ, đơn vị ngay sau nó)
    private final List<List<String>> headerRows;
    private final int codeColumn;
    private final int valueCount;
    private final List<Row> rows;

    @Getter
    @AllArgsConstructor
    public static class Row {
        private final String code;
        private final String name;
        // chuỗi gốc của từng cột giá trị, từ trái sang phải
        private final List<String> values;
    }
}
