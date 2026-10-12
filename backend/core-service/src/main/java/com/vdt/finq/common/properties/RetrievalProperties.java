package com.vdt.finq.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// cấu hình truy hồi, đọc từ khối retrieval trong application.yml
@Configuration
@ConfigurationProperties(prefix = "retrieval")
@Getter
@Setter
@NoArgsConstructor
public class RetrievalProperties {
    // số chunk lấy từ Qdrant (paper không nêu; ta chọn 80 vì kho nhỏ và đã lọc, QĐ-030)
    private int topK = 80;
    // số chunk giữ lại sau rerank tầng 1 (model nhỏ loại một nửa)
    private int tier1Keep = 40;
    // số chunk giữ lại sau rerank tầng 2, đưa vào LLM (paper: 20)
    private int finalK = 20;
    // lọc chặt mà ít hơn số này thì bỏ lọc, chỉ giữ ma_cong_ty (lọc sai năm/kỳ không được làm mất đáp án)
    private int minFilteredHits = 5;
    // tiền tố hướng dẫn của Qwen3-Embedding cho phía câu hỏi; để trống thì không thêm
    private String queryInstruction = "Given a Vietnamese question about a construction company's financial "
            + "reports, retrieve the report passages that answer it";
}
