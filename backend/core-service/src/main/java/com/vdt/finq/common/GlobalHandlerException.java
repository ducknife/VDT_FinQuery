package com.vdt.finq.common;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.vdt.finq.common.exception.AppException;

// đổi exception thành response JSON { code, message } với đúng mã HTTP
@RestControllerAdvice
public class GlobalHandlerException {

    // AppException mang sẵn mã HTTP (400, 404, 409, ...)
    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, Object>> handleApp(AppException e) {
        return ResponseEntity.status(e.getErrorCode())
                .body(Map.of("code", e.getErrorCode(), "message", e.getMessage()));
    }

    // file vượt spring.servlet.multipart.max-file-size
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTooLarge(MaxUploadSizeExceededException e) {
        int code = HttpStatus.PAYLOAD_TOO_LARGE.value();
        return ResponseEntity.status(code).body(Map.of("code", code, "message", "file quá lớn"));
    }
}
