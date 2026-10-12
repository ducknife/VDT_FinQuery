package com.vdt.finq.modules.hierarchicalrag.retrieval;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.finq.common.exception.InvalidRequestException;

import lombok.RequiredArgsConstructor;

// thử truy hồi bằng tay; tắt từng bước bằng tham số, ví dụ ?filter=false
@RestController
@RequestMapping("/api/v1/retrieval")
@RequiredArgsConstructor
public class RetrievalController {

    private final RetrievalService retrievalService;

    @GetMapping("/search")
    public RetrievalResult search(@RequestParam String q,
            @RequestParam(defaultValue = "true") boolean keywords,
            @RequestParam(defaultValue = "true") boolean filter,
            @RequestParam(defaultValue = "true") boolean rerank) {
        if (q.isBlank()) {
            throw new InvalidRequestException("câu hỏi trống");
        }
        return retrievalService.retrieve(q.strip(), new RetrievalOptions(keywords, filter, rerank));
    }
}
