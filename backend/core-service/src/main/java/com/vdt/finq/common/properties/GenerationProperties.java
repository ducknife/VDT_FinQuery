package com.vdt.finq.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// bước sinh câu trả lời, đọc từ khối generation trong application.yml
@Configuration
@ConfigurationProperties(prefix = "generation")
@Getter
@Setter
@NoArgsConstructor
public class GenerationProperties {
    // paper: câu hỏi + ngữ cảnh vượt ngưỡng này thì chia bằng chứng làm 2 nửa rồi hợp nhất
    private int contextLimitTokens = 64000;
}
