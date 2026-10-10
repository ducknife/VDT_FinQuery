package com.vdt.finq.modules.document;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.modules.document.dto.DocumentStatusResponse;
import com.vdt.finq.modules.document.dto.UploadDocumentRequest;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.DocumentIngestionService;

import lombok.RequiredArgsConstructor;

// tải tài liệu lên và theo dõi trạng thái nạp.
// OCR một tài liệu mất vài phút nên mọi lệnh nạp đều trả 202 ngay, việc chạy ở nền;
// giao diện hỏi GET /{id} để biết đã xong chưa
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final DocumentIngestionService documentIngestionService;

    // multipart: file + các trường của UploadDocumentRequest
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentStatusResponse upload(@RequestPart("file") MultipartFile file,
            @ModelAttribute UploadDocumentRequest request) throws IOException {
        Document saved = documentIngestionService.upload(file, request.toDocument());
        return DocumentStatusResponse.from(saved);
    }

    @GetMapping("/{taiLieuId}")
    public DocumentStatusResponse status(@PathVariable String taiLieuId) {
        return documentRepository.findById(taiLieuId)
                .map(DocumentStatusResponse::from)
                .orElseThrow(() -> new AppException(404, "không có tài liệu " + taiLieuId));
    }

    // chạy lại một tài liệu: OCR các trang còn thiếu (nếu chưa xong) rồi nạp lại Qdrant
    @PostMapping("/{taiLieuId}/index")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> reindex(@PathVariable String taiLieuId) {
        if (!documentRepository.existsById(taiLieuId)) {
            throw new AppException(404, "không có tài liệu " + taiLieuId);
        }
        documentIngestionService.submit(taiLieuId);
        return Map.of("taiLieuId", taiLieuId, "status", "queued");
    }

    // trích lại số liệu BCTC của cả kho vào financial_figures; kết quả và ngoại lệ xem trong log
    @PostMapping("/extract-figures")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> extractFigures() {
        documentIngestionService.submitExtractFigures();
        return Map.of("status", "queued");
    }

    // nạp mọi tài liệu đã OCR mà chưa embed; theo dõi tiến độ trong log
    @PostMapping("/index-pending")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> indexPending() {
        long pending = documentRepository.findByNgayOcrIsNotNullAndNgayEmbedIsNullOrderByTaiLieuId().size();
        documentIngestionService.submitIndexPending();
        return Map.of("pending", pending, "status", "queued");
    }
}
