package com.vdt.finq.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

// khối ingestion: trong application.yml
@Configuration
@ConfigurationProperties(prefix = "ingestion")
@Getter
@Setter
public class IngestionProperties {
    // thư mục file gốc; file của tài liệu nằm ở <raw-dir>/<ma_cong_ty>/<duong_dan>.<đuôi>
    private String rawDir = "../../data/raw";
    // thư mục con (trong thư mục của công ty) chứa tài liệu tải lên qua API
    private String uploadDir = "tai_len";
}
