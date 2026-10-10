package com.vdt.finq.modules.financial;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.vdt.finq.modules.document.Document;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.document.DocumentRepository;
import com.vdt.finq.modules.financial.extraction.DocumentFigureExtractor;
import com.vdt.finq.modules.financial.extraction.ExtractionIssue;
import com.vdt.finq.modules.financial.extraction.FigureCandidate;
import com.vdt.finq.modules.financial.extraction.FigureSelector;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.DocumentPageReader;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.stitcher.TableStitcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// trích số từ mọi BCTC và văn bản đính chính đã OCR vào financial_figures.
//
// chạy lại TOÀN BỘ mỗi lần chứ không từng tài liệu: chọn nguồn có thẩm quyền cần ứng viên của
// mọi tài liệu cùng lúc (báo cáo năm mới tải lên có thể thay số của báo cáo quý cũ).
// chỉ đọc file trên máy, không gọi API, cả kho mất vài giây, nên rẻ hơn nhiều so với
// lưu ứng viên trung gian rồi tính lại từng phần
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialExtractionService {

    // chỉ báo cáo gốc và đính chính; BCTN chép lại bảng nên không dùng (luong-hoi-dap_v1.0)
    private static final Set<LoaiTaiLieu> SOURCES = EnumSet.of(LoaiTaiLieu.BCTC, LoaiTaiLieu.DINH_CHINH);

    private final DocumentRepository documentRepository;
    private final DocumentPageReader pageReader;
    private final TableStitcher tableStitcher;
    private final FinancialCatalog catalog;
    private final FinancialFigureRepository store;

    public static boolean isSource(Document document) {
        return SOURCES.contains(document.getLoai());
    }

    public synchronized ExtractionSummary rebuildAll() {
        long start = System.nanoTime();
        FinancialCatalog.Snapshot snapshot = catalog.load();

        List<FigureCandidate> candidates = new ArrayList<>();
        List<ExtractionIssue> issues = new ArrayList<>();
        Map<String, Document> documents = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();
        int tables = 0;

        for (Document document : documentRepository.findByLoaiInAndNgayOcrIsNotNullOrderByTaiLieuId(SOURCES)) {
            // không biết kỳ hoặc phạm vi thì không gán được kỳ cho cột, bỏ qua cả tài liệu
            if (document.getNgayKetThucKy() == null || document.getPhamVi() == null) {
                skipped.add(document.getTaiLieuId());
                continue;
            }
            try {
                // bảng vắt sang trang sau thường không lặp lại dòng tiêu đề "Mã số | ... | 31/12/2023":
                // chép tiêu đề sang bằng đúng bộ sửa mà bước chunk dùng, để phần cuối B02 (mã 50-62) không bị bỏ sót
                List<String> pages = tableStitcher.repairTableHeaders(pageReader.readCleanPages(document));
                DocumentFigureExtractor.Result result =
                        DocumentFigureExtractor.extract(document, cheDo(document), pages, snapshot);
                candidates.addAll(result.getCandidates());
                issues.addAll(result.getIssues());
                tables += result.getTables();
                documents.put(document.getTaiLieuId(), document);
            } catch (Exception e) {
                log.error("trích số {} lỗi: {}", document.getTaiLieuId(), e.getMessage(), e);
                skipped.add(document.getTaiLieuId());
            }
        }

        FigureSelector.Selection selection = FigureSelector.select(candidates, documents);
        int written = store.replaceAll(selection.getSelected(), issues, documents.keySet());

        Map<String, Integer> byType = new TreeMap<>();
        issues.forEach(i -> byType.merge(i.getLoaiLoi().getValue(), 1, Integer::sum));
        int restated = (int) selection.getSelected().stream().filter(s -> s.getReplaced() != null).count();
        ExtractionSummary summary = new ExtractionSummary(documents.size(), tables, candidates.size(), written,
                restated, selection.getConflicts().size(), byType, skipped);
        selection.getConflicts().stream().limit(10).forEach(c -> log.info("khác số giữa các nguồn: {}", c));
        log.info("trích số xong trong {} ms: {}", (System.nanoTime() - start) / 1_000_000, summary);
        return summary;
    }

    // documents.che_do trống (văn bản đính chính) thì suy từ năm: TT99 áp dụng từ 2026
    private static String cheDo(Document document) {
        if (document.getCheDo() != null) {
            return document.getCheDo().getValue();
        }
        return document.getNam() >= 2026 ? "TT99" : "TT200";
    }
}
