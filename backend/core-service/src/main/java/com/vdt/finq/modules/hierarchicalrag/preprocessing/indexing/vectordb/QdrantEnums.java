package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.vectordb;

import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// các giá trị cố định chỉ có trong payload Qdrant (không có cột tương ứng trong MySQL).
// giá trị của tài liệu (ky, pham_vi, loai) nằm ở modules/document/DocumentEnums
public final class QdrantEnums {

    private QdrantEnums() {
    }

    // kiểu nội dung của một chunk
    @Getter
    @RequiredArgsConstructor
    public enum ElementType {
        TEXT("text"),   // chỉ có chữ
        TABLE("table"), // chỉ có bảng
        MIXED("mixed"); // cả chữ lẫn bảng

        private final String value;

        public static ElementType fromValue(String value) {
            return parse(values(), ElementType::getValue, value);
        }
    }

    // chuỗi -> enum; null giữ null, chuỗi lạ thì báo lỗi
    private static <E extends Enum<E>> E parse(E[] values, Function<E, String> toValue, String value) {
        if (value == null) {
            return null;
        }
        for (E e : values) {
            if (toValue.apply(e).equals(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("giá trị không có trong enum: " + value);
    }
}
