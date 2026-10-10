package com.vdt.finq.modules.financial;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.finq.modules.financial.FinancialEnums.CachLay;
import com.vdt.finq.modules.financial.FinancialEnums.TrangThaiSo;
import com.vdt.finq.modules.financial.extraction.ExtractionIssue;
import com.vdt.finq.modules.financial.extraction.FigureCandidate;
import com.vdt.finq.modules.financial.extraction.FigureSelector.Selected;

import lombok.RequiredArgsConstructor;

// ghi kết quả trích số. dùng JdbcTemplate chứ không dùng JPA: ghi hàng nghìn dòng một lượt,
// và nhánh truy vấn DB sau này cũng chạy SQL mẫu có tham số (paper FinAI Data Assistant)
@Repository
@RequiredArgsConstructor
public class FinancialFigureRepository {

    private static final String INSERT_FIGURE = "INSERT INTO financial_figures "
            + "(ma_noi_bo, tu_ngay, den_ngay, pham_vi, gia_tri, tai_lieu_id, trang, cach_lay, trang_thai, "
            + "thay_the_id, ly_do_thay_the) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ISSUE = "INSERT INTO extraction_exceptions "
            + "(tai_lieu_id, trang, loai_loi, che_do, mau_bieu, ma, ten_doc_duoc, ten_ky_vong, gia_tri_doc, chi_tiet) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    // thay TOÀN BỘ kết quả cũ trong một giao dịch: trích số là hàm tất định của kho tài liệu,
    // chạy lại luôn ra cùng kết quả. lỗi giữa chừng thì quay lui, bảng cũ còn nguyên.
    // ngoại lệ người soát đã xử lý (da_xu_ly = 1) được giữ lại vì có ghi chú của người
    @Transactional
    public int replaceAll(List<Selected> selected, List<ExtractionIssue> issues, Collection<String> extractedDocuments) {
        jdbcTemplate.update("DELETE FROM financial_figures");
        jdbcTemplate.update("DELETE FROM extraction_exceptions WHERE da_xu_ly = 0");

        List<Object[]> plain = new ArrayList<>();
        int written = 0;
        for (Selected s : selected) {
            if (s.getReplaced() == null) {
                plain.add(figureArgs(s.getWinner(), TrangThaiSo.HIEU_LUC, null, null));
                continue;
            }
            // số gốc ghi trước để lấy id, số trình bày lại trỏ về nó
            long oldId = insertReturningId(figureArgs(s.getReplaced(), TrangThaiSo.DA_THAY_THE, null, null));
            String reason = "trình bày lại tại " + s.getWinner().getTaiLieuId() + " trang " + s.getWinner().getTrang();
            jdbcTemplate.update(INSERT_FIGURE, figureArgs(s.getWinner(), TrangThaiSo.HIEU_LUC, oldId, reason));
            written += 2;
        }
        written += jdbcTemplate.batchUpdate(INSERT_FIGURE, plain).length;

        List<Object[]> issueArgs = new ArrayList<>();
        for (ExtractionIssue i : issues) {
            issueArgs.add(new Object[] {i.getTaiLieuId(), i.getTrang(), i.getLoaiLoi().getValue(), i.getCheDo(),
                    i.getMauBieu(), cut(i.getMa(), 4), cut(i.getTenDocDuoc(), 200), cut(i.getTenKyVong(), 200),
                    cut(i.getGiaTriDoc(), 64), cut(i.getChiTiet(), 500)});
        }
        jdbcTemplate.batchUpdate(INSERT_ISSUE, issueArgs);

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> marks = extractedDocuments.stream().map(id -> new Object[] {now, id}).toList();
        jdbcTemplate.batchUpdate("UPDATE documents SET ngay_trich_so = ? WHERE tai_lieu_id = ?", marks);
        return written;
    }

    private long insertReturningId(Object[] args) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(INSERT_FIGURE, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    ps.setNull(i + 1, Types.NULL);
                } else {
                    ps.setObject(i + 1, args[i]);
                }
            }
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    private static Object[] figureArgs(FigureCandidate c, TrangThaiSo trangThai, Long thayTheId, String lyDo) {
        CachLay cachLay = c.isCurrent() ? CachLay.COT_KY_NAY : CachLay.COT_KY_TRUOC;
        return new Object[] {c.getMaNoiBo(), c.getPeriod().getTuNgay(), c.getPeriod().getDenNgay(), c.getPhamVi(),
                c.getGiaTri(), c.getTaiLieuId(), c.getTrang(), cachLay.getValue(), trangThai.getValue(), thayTheId,
                lyDo};
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
