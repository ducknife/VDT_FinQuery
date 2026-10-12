package com.vdt.finq.modules.hierarchicalrag.generation;

import lombok.AllArgsConstructor;
import lombok.Getter;

// một trích dẫn: mã bằng chứng, nguồn, câu trích và đã kiểm thấy trong nguồn chưa
@Getter
@AllArgsConstructor
public class Citation {
    private final String id;
    private final String taiLieuId;
    private final String tenHienThi;
    private final int trang;
    private final String quote;
    // câu trích có thật trong bằng chứng (so sau khi bỏ ký hiệu bảng, gộp khoảng trắng)
    private final boolean verified;
}
