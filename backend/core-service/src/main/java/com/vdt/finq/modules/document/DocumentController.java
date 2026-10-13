package com.vdt.finq.modules.document;

import java.io.IOException;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.exception.InvalidRequestException;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.document.dto.DocumentStatusResponse;
import com.vdt.finq.modules.document.dto.UploadDocumentRequest;
import com.vdt.finq.modules.rag.preprocessing.DocumentIngestionService;

import lombok.RequiredArgsConstructor;

// tải tài liệu lên và theo dõi trạng thái nạp; nạp chạy nền, trả 202 ngay
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final DocumentIngestionService documentIngestionService;
    private final DocumentPageService documentPageService;

    // multipart: file + các trường của UploadDocumentRequest
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentStatusResponse upload(@RequestPart("file") MultipartFile file,
            @ModelAttribute UploadDocumentRequest request) throws IOException {
        Document saved = documentIngestionService.upload(file, request.toDocument());
        return DocumentStatusResponse.from(saved);
    }

    // danh sách cho màn hình quản lý tài liệu: mới ban hành trước
    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String q, @RequestParam(required = false) String loai,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        LoaiTaiLieu type;
        try {
            type = loai == null || loai.isBlank() ? null : LoaiTaiLieu.fromValue(loai);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
        Page<Document> result = documentRepository.search(q == null || q.isBlank() ? null : q.strip(), type,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                        Sort.by(Sort.Order.desc("ngayBanHanh"), Sort.Order.asc("taiLieuId"))));
        return Map.of("items", result.getContent().stream().map(DocumentStatusResponse::from).toList(),
                "total", result.getTotalElements(), "page", result.getNumber(), "size", result.getSize());
    }

    @GetMapping("/{taiLieuId}")
    public DocumentStatusResponse status(@PathVariable String taiLieuId) {
        return documentRepository.findById(taiLieuId)
                .map(DocumentStatusResponse::from)
                .orElseThrow(() -> new AppException(404, "không có tài liệu " + taiLieuId));
    }

    // bấm trích dẫn: ảnh trang PDF gốc
    @GetMapping(value = "/{taiLieuId}/pages/{trang}/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] pageImage(@PathVariable String taiLieuId, @PathVariable int trang) {
        return documentPageService.pageImage(taiLieuId, trang);
    }

    // bấm trích dẫn: chữ OCR của trang (markdown)
    @GetMapping(value = "/{taiLieuId}/pages/{trang}/text", produces = "text/markdown;charset=UTF-8")
    public String pageText(@PathVariable String taiLieuId, @PathVariable int trang) {
        return documentPageService.pageText(taiLieuId, trang);
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
