package com.vdt.finq.modules.agent.tools.financial.support;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.finq.common.db.ReadOnlyJdbc;

// SQL hằng số cho tool số liệu (FinAI Data Assistant): LLM chỉ điền giá trị vào dấu ?.
// chạy bằng kết nối chỉ đọc (ReadOnlyJdbc), trong giao dịch READ ONLY
@Repository
public class FigureQueryRepository {

    // S1: một chỉ tiêu, một kỳ, một phạm vi; kèm số cũ đã bị trình bày lại
    private static final String FIND_FIGURE = "SELECT f.gia_tri, f.trang, f.trang_thai, f.ly_do_thay_the, "
            + "d.tai_lieu_id, d.ten_hien_thi FROM financial_figures f "
            + "JOIN documents d ON d.tai_lieu_id = f.tai_lieu_id "
            + "WHERE f.ma_noi_bo = ? AND f.pham_vi = ? AND f.tu_ngay = ? AND f.den_ngay = ? "
            + "ORDER BY (f.trang_thai = 'hieu_luc') DESC, f.id";

    // S2: kỳ mới nhất trong kho của một phạm vi
    private static final String LATEST = "SELECT MAX(den_ngay) FROM financial_figures "
            + "WHERE pham_vi = ? AND trang_thai = 'hieu_luc'";

    // S3: các kỳ đang có của một chỉ tiêu (gợi ý khi kỳ được hỏi chưa có)
    private static final String AVAILABLE = "SELECT DISTINCT tu_ngay, den_ngay FROM financial_figures "
            + "WHERE ma_noi_bo = ? AND pham_vi = ? AND trang_thai = 'hieu_luc' ORDER BY den_ngay DESC, tu_ngay LIMIT 8";

    // từ điển chỉ tiêu: nạp một lần
    private static final String ITEMS = "SELECT ma_noi_bo, ten_chuan, loai_so, thay_the_bang FROM financial_items "
            + "ORDER BY ma_noi_bo";

    public record Row(long value, int trang, boolean current, String reason, String taiLieuId, String tenHienThi) {
    }

    public record ItemRow(String code, String name, boolean pointInTime, String replacedBy) {
    }

    private final JdbcTemplate jdbc;

    public FigureQueryRepository(ReadOnlyJdbc readOnlyJdbc) {
        this.jdbc = readOnlyJdbc.template();
    }

    @Transactional(readOnly = true)
    public List<Row> find(String code, String phamVi, LocalDate from, LocalDate to) {
        return jdbc.query(FIND_FIGURE, (rs, i) -> new Row(rs.getLong("gia_tri"), rs.getInt("trang"),
                "hieu_luc".equals(rs.getString("trang_thai")), rs.getString("ly_do_thay_the"),
                rs.getString("tai_lieu_id"), rs.getString("ten_hien_thi")),
                code, phamVi, Date.valueOf(from), Date.valueOf(to));
    }

    @Transactional(readOnly = true)
    public LocalDate latest(String phamVi) {
        Date d = jdbc.queryForObject(LATEST, Date.class, phamVi);
        return d == null ? null : d.toLocalDate();
    }

    @Transactional(readOnly = true)
    public List<LocalDate[]> available(String code, String phamVi) {
        List<LocalDate[]> out = new ArrayList<>();
        jdbc.query(AVAILABLE, rs -> {
            out.add(new LocalDate[] { rs.getDate("tu_ngay").toLocalDate(), rs.getDate("den_ngay").toLocalDate() });
        }, code, phamVi);
        return out;
    }

    @Transactional(readOnly = true)
    public List<ItemRow> items() {
        return jdbc.query(ITEMS, (rs, i) -> new ItemRow(rs.getString("ma_noi_bo"), rs.getString("ten_chuan"),
                "thoi_diem".equals(rs.getString("loai_so")), rs.getString("thay_the_bang")));
    }
}
