package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.vectordb;

import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// giá trị cố định chỉ có trong payload Qdrant; giá trị của tài liệu nằm ở DocumentEnums
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
