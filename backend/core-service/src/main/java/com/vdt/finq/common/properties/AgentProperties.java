package com.vdt.finq.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// khối agent trong application.yml
@Configuration
@ConfigurationProperties(prefix = "agent")
@Getter
@Setter
@NoArgsConstructor
public class AgentProperties {
    // tổng số lần gọi LLM mỗi câu: (maxTurnLoop - 1) lượt chọn tool + 1 lượt viết câu trả lời
    private int maxTurnLoop = 4;
}
