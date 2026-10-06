package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.strategy;

import java.io.IOException;
import java.nio.file.Path;

import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.DocumentOcrResult;

// one implementation per file format, add a format by adding a @Component
public interface DocumentExtractor {

    // must be cheap, do not open the file here
    boolean supports(String fileName);

    // every implementation writes through PageStore so the rest of the pipeline
    // only ever sees numbered .md files, whatever the source format was
    DocumentOcrResult extract(Path file, String relativePath) throws IOException;
}
