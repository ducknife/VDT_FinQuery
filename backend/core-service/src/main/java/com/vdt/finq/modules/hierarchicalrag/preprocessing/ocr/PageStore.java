package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import java.io.IOException;
import java.util.Optional;

// Interface check documents 
// check if this document is existed 
// write content to file in path
// read this document, return String text
// mark failed if can't and give a reason 

public interface PageStore {
    boolean exists(String relativePath, int pageNumber) throws IOException; 
    void write(String relativePath, int pageNumber, String markdown) throws IOException;
    Optional<String> read(String relativePath, int pageNumber) throws IOException;
    void markFailed(String relativePath, int pageNumber, String reason) throws IOException;
}
