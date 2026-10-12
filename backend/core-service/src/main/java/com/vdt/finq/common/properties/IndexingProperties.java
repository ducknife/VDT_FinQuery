package com.vdt.finq.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// cấu hình cho bước lập chỉ mục, đọc từ khối indexing trong application.yml
@Configuration
@ConfigurationProperties(prefix = "indexing")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IndexingProperties {

    // mức trần để quyết định có cắt trang hay không, tính bằng ký tự sau khi làm sạch
    private int maxChunkChars = 4000;

    // độ dài tối đa của mẩu câu được gối sang trang kề bên
    private int stitchMaxChars = 500;

    // giới hạn của model embedding (khoảng 32k token), chỉ cắt cứng khi vượt mức này
    private int modelLimitChars = 50000;

    // bảng đem embed giữ markdown hay đổi sang JSON; đổi thì phải dùng collection mới và nạp lại
    private TableFormat tableFormat = TableFormat.MARKDOWN;

    // chuẩn hoá văn bản đem embed (QĐ-027), mặc định tắt; bật thì đổi QDRANT_COLLECTION
    private boolean corpusNormalization = false;

    public enum TableFormat {
        MARKDOWN,
        JSON
    }
}
