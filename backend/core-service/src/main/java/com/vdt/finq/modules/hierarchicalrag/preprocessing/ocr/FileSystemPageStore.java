package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.vdt.finq.common.properties.OCRProperties;

import lombok.RequiredArgsConstructor;


// store page of document when render 
@Component
@RequiredArgsConstructor
public class FileSystemPageStore implements PageStore {

    private final OCRProperties ocrProperties;

    // if the path is existed -> return true
    @Override
    public boolean exists(String relativePath, int pageNumber) throws IOException {
        Path resolvedPath = resolve(relativePath, pageNumber);
        return Files.exists(resolvedPath) && Files.size(resolvedPath) > 0;
    }

    // resolve the path and write to it
    @Override
    public void write(String relativePath, int pageNumber, String markdown) throws IOException {
        Path resolvedPath = resolve(relativePath, pageNumber);
        Files.createDirectories(resolvedPath.getParent());

        // write to tmp file, and rename
        // avoid uncompleted data if cancel imeditately
        Path tmp = resolvedPath.resolveSibling(resolvedPath.getFileName() + ".tmp");
        Files.writeString(tmp, markdown, StandardCharsets.UTF_8);
        Files.move(tmp, resolvedPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        // delete error file if the next time success
        Files.deleteIfExists(resolvedPath.resolveSibling(resolvedPath.getFileName() + ".loi"));
    }

    // if file not found or is empty()
    @Override
    public Optional<String> read(String relativePath, int pageNumber) throws IOException {
        Path resolvedPath = resolve(relativePath, pageNumber);
        if (!Files.exists(resolvedPath))
            return Optional.empty();
        String markdown = Files.readString(resolvedPath);
        if (markdown.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(markdown);
    }

    // create error file include time and reason
    @Override
    public void markFailed(String relativePath, int pageNumber, String reason) throws IOException {
        Path resolvedPath = resolve(relativePath, pageNumber);
        String content = Instant.now() + " " + reason;
        Path errorPath = resolvedPath.resolveSibling(resolvedPath.getFileName() + ".loi");
        Files.createDirectories(errorPath.getParent());
        Files.writeString(errorPath, content, StandardCharsets.UTF_8);
    }

    // resolve the path from String to Path
    private Path resolve(String relativePath, int pageNumber) {
        Path base = Path.of(ocrProperties.getOutputDir()).toAbsolutePath().normalize();
        Path dir = base.resolve(relativePath).normalize();
        // ff it's not starting with base: data/ocr
        // trhow an error
        if (!dir.startsWith(base)) {
            throw new IllegalArgumentException("Path miss match!" + relativePath);
        }
        return dir.resolve(String.format("trang_%03d.md", pageNumber));
    }
}
