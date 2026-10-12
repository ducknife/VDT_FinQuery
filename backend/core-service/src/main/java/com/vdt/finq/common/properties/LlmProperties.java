package com.vdt.finq.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// model suy luận (kết nối ở spring.ai.openai): chế độ thinking và đơn giá; tiền = token × đơn giá
@Configuration
@ConfigurationProperties(prefix = "llm")
@Getter
@Setter
@NoArgsConstructor
public class LlmProperties {
    private String model;
    // chế độ thinking (chốt bật 12/10/2026), gửi qua chat_template_kwargs.enable_thinking
    private boolean thinking = true;
    private double priceInputPerMillion = 0.45;
    private double priceOutputPerMillion = 3.00;

    public double cost(int promptTokens, int completionTokens) {
        return (promptTokens * priceInputPerMillion + completionTokens * priceOutputPerMillion) / 1_000_000;
    }
}
