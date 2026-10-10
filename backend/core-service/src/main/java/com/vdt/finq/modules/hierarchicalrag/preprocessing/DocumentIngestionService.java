package com.vdt.finq.modules.hierarchicalrag.preprocessing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.exception.InvalidRequestException;
import com.vdt.finq.common.properties.IngestionProperties;
import com.vdt.finq.modules.document.Document;
import com.vdt.finq.modules.document.DocumentIdGenerator;
import com.vdt.finq.modules.document.DocumentRepository;
import com.vdt.finq.modules.financial.FinancialExtractionService;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner.NFCNormalization;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.DocumentIndexingService;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.DocumentOcrResult;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.strategy.ExtractionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// điều phối cả luồng nạp một tài liệu: lưu file -> tạo dòng documents -> OCR -> trích số -> lập chỉ mục.
// bước nặng chạy nền trên ingestionExecutor; mỗi bước chỉ chạy khi cột trạng thái của nó còn NULL,
// nên gọi lại process() sau khi lỗi sẽ làm tiếp đúng chỗ dở
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final IngestionProperties properties;
    private final DocumentRepository documentRepository;
    private final ExtractionService extractionService;
    private final DocumentIndexingService documentIndexingService;
    private final FinancialExtractionService financialExtractionService;
    private final NFCNormalization nfcNormalization;
    private final ExecutorService ingestionExecutor;

    // nhận file tải lên: kiểm tra, lưu file, tạo dòng documents, rồi xếp hàng xử lý nền
    public Document upload(MultipartFile file, Document document) throws IOException {
        String fileName = file.getOriginalFilename();
        if (file.isEmpty() || fileName == null) {
            throw new InvalidRequestException("file trống");
        }
        if (!extractionService.canExtract(fileName)) {
            throw new InvalidRequestException("không hỗ trợ định dạng của " + fileName);
        }

        // cổng NFC thứ ba (.docs/projectdevlogs/lam-sach): macOS gửi tên file dạng NFD, mà duong_dan so từng byte (utf8mb4_bin)
        // nên "riêng" NFD và NFC thành hai đường dẫn khác nhau, chặn trùng bị lọt.
        // tên hiển thị cũng chuẩn hoá vì nó nằm trong văn bản đem embed
        fileName = nfcNormalization.toNfc(fileName);
        document.setTenHienThi(nfcNormalization.toNfc(document.getTenHienThi()));

        // duong_dan: tai_len/<loai>/<nam>/<tên file đã làm sạch, không đuôi>
        document.setDuongDan(properties.getUploadDir() + "/" + document.getLoai().getValue()
                + "/" + document.getNam() + "/" + safeBaseName(fileName));
        if (documentRepository.existsByMaCongTyAndDuongDan(document.getMaCongTy(), document.getDuongDan())) {
            throw new AppException(409, "đã có tài liệu ở " + document.getDuongDan());
        }
        document.setTaiLieuId(DocumentIdGenerator.generate(document));

        // lưu file trước, ghi DB sau: lỗi giữa chừng chỉ để lại một file thừa, không để lại dòng trỏ vào hư không
        Path target = companyDir(document).resolve(document.getDuongDan() + "." + extension(fileName)).normalize();
        Files.createDirectories(target.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target);
        }
        // số trang chưa biết trước khi OCR; cột NOT NULL nên tạm để 0
        document.setTongSoTrang(0);
        Document saved = documentRepository.save(document);
        log.info("đã nhận {} -> {}", fileName, saved.getTaiLieuId());

        submit(saved.getTaiLieuId());
        return saved;
    }

    // xếp một tài liệu vào hàng xử lý nền (dùng cả khi muốn chạy lại tài liệu lỗi)
    public void submit(String taiLieuId) {
        ingestionExecutor.submit(() -> process(taiLieuId));
    }

    // xếp lượt nạp mọi tài liệu đã OCR mà chưa embed (278 tài liệu có sẵn)
    public void submitIndexPending() {
        ingestionExecutor.submit(documentIndexingService::indexPending);
    }

    // trích lại số liệu cả kho vào financial_figures (theo dõi kết quả trong log)
    public void submitExtractFigures() {
        ingestionExecutor.submit(financialExtractionService::rebuildAll);
    }

    // OCR (bỏ qua các trang đã có) -> trích số (BCTC, đính chính) -> lập chỉ mục
    private void process(String taiLieuId) {
        try {
            Document document = documentRepository.findById(taiLieuId)
                    .orElseThrow(() -> new IllegalStateException("không có tài liệu " + taiLieuId));

            if (document.getNgayOcr() == null) {
                DocumentOcrResult result = extractionService.extract(findRawFile(document), document.ocrRelativePath());
                if (!result.isComplete()) {
                    // còn trang lỗi: không ghi ngay_ocr, gọi submit lại sẽ chỉ OCR các trang còn thiếu
                    log.warn("{}: OCR còn {} trang lỗi, dừng trước bước lập chỉ mục", taiLieuId, result.getFailed());
                    return;
                }
                documentRepository.updateOcrState(taiLieuId, LocalDateTime.now(), result.getTotalPages());
            }

            // trích số lỗi không chặn lập chỉ mục: hai nhánh độc lập, trang vẫn tìm được qua RAG
            if (FinancialExtractionService.isSource(document) && document.getNgayTrichSo() == null) {
                try {
                    financialExtractionService.rebuildAll();
                } catch (RuntimeException e) {
                    log.error("trích số sau khi nạp {} lỗi: {}", taiLieuId, e.getMessage(), e);
                }
            }

            documentIndexingService.indexDocument(taiLieuId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // chạy nền nên không có ai nhận exception: phải ghi log, trạng thái thì xem qua GET /documents/{id}
            log.error("xử lý {} lỗi: {}", taiLieuId, e.getMessage(), e);
        }
    }

    // thư mục file gốc của công ty: <raw-dir>/<ma_cong_ty>
    private Path companyDir(Document document) {
        return Path.of(properties.getRawDir()).toAbsolutePath().normalize().resolve(document.getMaCongTy());
    }

    // file gốc nằm ở <raw-dir>/<ma_cong_ty>/<duong_dan>.<đuôi>; đuôi không lưu trong DB nên tìm theo tên
    private Path findRawFile(Document document) throws IOException {
        Path base = companyDir(document).resolve(document.getDuongDan()).normalize();
        String prefix = base.getFileName() + ".";
        try (Stream<Path> files = Files.list(base.getParent())) {
            return files.filter(p -> p.getFileName().toString().startsWith(prefix))
                    .filter(p -> extractionService.canExtract(p.getFileName().toString()))
                    .findFirst()
                    .orElseThrow(() -> new IOException("không thấy file gốc của " + document.getTaiLieuId()));
        }
    }

    // bỏ thư mục và đuôi, chỉ giữ chữ, số, dấu chấm, gạch: chặn path traversal kiểu ../../
    private static String safeBaseName(String fileName) {
        String name = Path.of(fileName).getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String safe = base.replaceAll("[^\\p{L}\\p{N}._-]+", "-").replaceAll("^[.-]+", "");
        if (safe.isEmpty()) {
            throw new InvalidRequestException("tên file không hợp lệ: " + fileName);
        }
        return safe;
    }

    private static String extension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
