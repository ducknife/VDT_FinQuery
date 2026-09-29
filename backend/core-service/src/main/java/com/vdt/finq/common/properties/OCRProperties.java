package com.vdt.finq.common.properties;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Configuration 
@ConfigurationProperties (prefix = "ocr")
@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
public class OCRProperties {
    
    private String outputDir; 
    private String model; 
    private String apiUrl;
    private String apiKey; 
    private int dpi; 
    private double jpegQuality; 
    private int concurrency; 
    private Long maxTokens; 
    private Duration firstChunkTimeout; 
    private Duration silenceTimeout; 
    private Duration totalTimeout; 
    private int maxAttempts; 
    private List<Integer> dpiLadder; 
}
