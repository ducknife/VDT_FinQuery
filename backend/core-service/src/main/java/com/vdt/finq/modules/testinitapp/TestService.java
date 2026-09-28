package com.vdt.finq.modules.testinitapp;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class TestService {

    private final TestRepository testRepository;
    
    public List<Test> getTests() {
        return testRepository.findAll();
    }
}
