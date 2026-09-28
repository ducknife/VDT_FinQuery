package com.vdt.finq.modules.testinitapp;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor 
public class TestController {

    private final TestService testService;
    
    @GetMapping
    public Map<String, List<Test>> getTests() {
        return Map.of(
            "Data", testService.getTests()
        );
    }
}
