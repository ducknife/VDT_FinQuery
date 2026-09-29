package com.vdt.finq.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import com.vdt.finq.common.properties.OCRProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration 
@RequiredArgsConstructor 
@Slf4j 
public class RunnerTest implements CommandLineRunner {

    private final OCRProperties ocrProperties;
    
    @Override 
    public void run(String ... args) {

        log.info(ocrProperties.getDpiLadder().toString());
        
    }
}
