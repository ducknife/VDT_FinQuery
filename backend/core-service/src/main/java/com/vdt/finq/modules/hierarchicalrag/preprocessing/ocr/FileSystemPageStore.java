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


// lưu kết quả OCR của từng trang ra đĩa, mỗi trang một file trang_NNN.md
@Component
@RequiredArgsConstructor
public class FileSystemPageStore implements PageStore {

    private final OCRProperties ocrProperties;

    // trả về true nếu file của trang đã có và không rỗng
    @Override
    public boolean exists(String relativePath, int pageNumber) throws IOException {
        Path resolvedPath = resolve(relativePath, pageNumber);
        return Files.exists(resolvedPath) && Files.size(resolvedPath) > 0;
    }

    // tìm đường dẫn của trang rồi ghi nội dung vào
    @Override
    public void write(String relativePath, int pageNumber, String markdown) throws IOException {
        Path resolvedPath = resolve(relativePath, pageNumber);
        Files.createDirectories(resolvedPath.getParent());

        // ghi ra file .tmp rồi đổi tên
        // tránh để lại file dở dang nếu bị ngắt giữa chừng
        Path tmp = resolvedPath.resolveSibling(resolvedPath.getFileName() + ".tmp");
        Files.writeString(tmp, markdown, StandardCharsets.UTF_8);
        Files.move(tmp, resolvedPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        // lần sau thành công thì xoá file .loi cũ
        Files.deleteIfExists(resolvedPath.resolveSibling(resolvedPath.getFileName() + ".loi"));
    }

    // trả về rỗng nếu không có file hoặc file trống
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

    // tạo file .loi ghi thời điểm và lý do lỗi
    @Override
    public void markFailed(String relativePath, int pageNumber, String reason) throws IOException {
        Path resolvedPath = resolve(relativePath, pageNumber);
        String content = Instant.now() + " " + reason;
        Path errorPath = resolvedPath.resolveSibling(resolvedPath.getFileName() + ".loi");
        Files.createDirectories(errorPath.getParent());
        Files.writeString(errorPath, content, StandardCharsets.UTF_8);
    }

    // đổi đường dẫn tương đối thành Path tới file của trang
    private Path resolve(String relativePath, int pageNumber) {
        Path base = Path.of(ocrProperties.getOutputDir()).toAbsolutePath().normalize();
        Path dir = base.resolve(relativePath).normalize();
        // đường dẫn thoát ra ngoài thư mục gốc data/ocr thì báo lỗi
        // (chặn path traversal)
        if (!dir.startsWith(base)) {
            throw new IllegalArgumentException("Path miss match!" + relativePath);
        }
        return dir.resolve(String.format("trang_%03d.md", pageNumber));
    }
}
