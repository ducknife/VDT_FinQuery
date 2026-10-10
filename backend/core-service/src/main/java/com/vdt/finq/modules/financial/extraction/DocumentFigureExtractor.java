package com.vdt.finq.modules.financial.extraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vdt.finq.common.markdown.MarkdownTableParser;
import com.vdt.finq.common.markdown.MarkdownTableParser.Block;
import com.vdt.finq.common.text.VietnameseText;
import com.vdt.finq.modules.document.Document;
import com.vdt.finq.modules.financial.FinancialCatalog;
import com.vdt.finq.modules.financial.FinancialEnums.LoaiLoi;
import com.vdt.finq.modules.financial.FinancialEnums.LoaiSo;
import com.vdt.finq.modules.financial.FinancialEnums.MauBieu;

import lombok.Getter;

// trích số từ MỘT tài liệu: tìm bảng B01, B02, B03 -> gán kỳ cho cột -> tra mã nội bộ
// -> kiểm tên, dấu, phép cộng. số qua hết các chốt thành ứng viên; số trượt thành ExtractionIssue.
// chưa chọn nguồn có thẩm quyền: việc đó cần ứng viên của MỌI tài liệu (FigureSelector)
public final class DocumentFigureExtractor {

    @Getter
    public static class Result {
        private final List<FigureCandidate> candidates = new ArrayList<>();
        private final List<ExtractionIssue> issues = new ArrayList<>();
        private int tables;
    }

    private final Document document;
    private final String cheDo;
    private final FinancialCatalog.Snapshot catalog;
    private final Result result = new Result();

    // bảng tiếp theo trang trước không ghi lại tên mẫu biểu: mang theo mẫu biểu gần nhất
    private MauBieu lastForm;
    private MauBieu lastCashFlowForm;

    // bảng mẫu biểu gần nhất: bảng đầu trang sau mất tiêu đề thì dùng lại kỳ của các cột
    private ColumnPlanner.Plan lastPlan;
    private MauBieu lastPlanForm;
    private long lastUnit = 1;
    private int lastPage = -1;

    private DocumentFigureExtractor(Document document, String cheDo, FinancialCatalog.Snapshot catalog) {
        this.document = document;
        this.cheDo = cheDo;
        this.catalog = catalog;
    }

    // cheDo: TT200 hoặc TT99 (bên gọi suy từ năm khi documents.che_do trống)
    public static Result extract(Document document, String cheDo, List<String> pages,
            FinancialCatalog.Snapshot catalog) {
        DocumentFigureExtractor extractor = new DocumentFigureExtractor(document, cheDo, catalog);
        List<FigureCandidate> read = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            extractor.readPage(i + 1, pages.get(i), read);
        }
        extractor.checkConsistency(read);
        return extractor.result;
    }

    private void readPage(int page, String text, List<FigureCandidate> out) {
        if (text.isEmpty() || !text.contains("|")) {
            return;
        }
        StringBuilder context = new StringBuilder();
        boolean firstTable = true;
        for (Block block : MarkdownTableParser.split(text)) {
            if (!block.isTable()) {
                context.append(block.getText()).append('\n');
                continue;
            }
            StatementTable table = StatementTableReader.read(block.getTable());
            // mục thuyết minh "Số liệu so sánh" đối chiếu số cũ và số phân loại lại, không phải mẫu biểu
            if (table != null && !VietnameseText.fold(context.toString()).contains("so lieu so sanh")) {
                readTable(page, table, context + "\n" + leadText(block.getTable()), text, out);
            } else if (table == null && firstTable && lastPlan != null && lastPage == page - 1) {
                // bảng đầu trang mất dòng tiêu đề, ngay sau một mẫu biểu ở trang trước: phần tiếp theo của nó
                StatementTable rest = StatementTableReader.readContinuation(block.getTable(),
                        lastPlan.getColumns().size());
                // bảng thuyết minh đầu trang sau mẫu biểu cuối cùng cũng không có tiêu đề:
                // chỉ nhận khi đa số dòng khớp (mã, tên) của đúng mẫu biểu đang đọc dở
                if (rest != null && matchShare(rest, lastPlanForm) >= 0.5) {
                    result.tables++;
                    readRows(page, rest, lastPlanForm, lastPlan, lastUnit, out);
                }
            }
            firstTable = false;
            context.setLength(0);
        }
    }

    private void readTable(int page, StatementTable table, String context, String pageText, List<FigureCandidate> out) {
        result.tables++;
        String headerText = headerText(table);
        MauBieu form = checkByCodes(detectForm(context + "\n" + headerText + "\n" + firstNames(table), pageText, table),
                table);
        if (form == null) {
            issue(page, LoaiLoi.THIEU_MAU_BIEU, null, null, null, null, null,
                    "bảng có cột mã số nhưng không nhận ra mẫu biểu");
            return;
        }

        ColumnPlanner.Plan plan = ColumnPlanner.plan(form == MauBieu.B01, document.getNgayKetThucKy(),
                table, context + "\n" + headerText);
        if (plan.getColumns() == null) {
            issue(page, LoaiLoi.KHONG_XAC_DINH_KY, form, null, null, null, null, plan.getReason());
            return;
        }
        long unit = unit(context + "\n" + headerText);
        lastPlan = plan;
        lastPlanForm = form;
        lastUnit = unit;
        readRows(page, table, form, plan, unit, out);
    }

    // mỗi dòng: tra mã, kiểm tên, đọc số từng cột, kiểm dấu
    private void readRows(int page, StatementTable table, MauBieu form, ColumnPlanner.Plan plan, long unit,
            List<FigureCandidate> out) {
        lastPage = page;
        for (StatementTable.Row row : table.getRows()) {
            // dòng con có chữ cái (411a, 421b) chỉ là chi tiết, bảng ánh xạ không theo dõi
            if (!Character.isDigit(row.getCode().charAt(row.getCode().length() - 1))) {
                continue;
            }
            FinancialCatalog.Mapping mapping = catalog.mapping(cheDo, form, row.getCode());
            if (mapping == null) {
                issue(page, LoaiLoi.MA_LA, form, row.getCode(), row.getName(), null, first(row), null);
                continue;
            }
            if (!NameMatcher.matches(row.getName(), mapping.getTenTrenBaoCao())) {
                issue(page, LoaiLoi.TEN_KHONG_KHOP, form, row.getCode(), row.getName(),
                        mapping.getTenTrenBaoCao(), first(row), null);
                continue;
            }
            FinancialCatalog.Item item = catalog.item(mapping.getMaNoiBo());
            for (int j = 0; j < plan.getColumns().size(); j++) {
                ColumnPlanner.Column column = plan.getColumns().get(j);
                String raw = row.getValues().get(j);
                Long value;
                try {
                    value = VietnameseNumberParser.parse(raw);
                } catch (NumberFormatException e) {
                    issue(page, LoaiLoi.KHONG_DOC_DUOC_SO, form, row.getCode(), row.getName(), null, raw,
                            "cột " + column.getPeriod());
                    continue;
                }
                if (value == null) {
                    continue;
                }
                value *= unit;
                if (item.isDauAmTuNhien() && value > 0) {
                    issue(page, LoaiLoi.SAI_DAU, form, row.getCode(), row.getName(), null, raw,
                            "chỉ tiêu bản chất âm mà đọc ra số dương, cột " + column.getPeriod());
                    continue;
                }
                out.add(new FigureCandidate(mapping.getMaNoiBo(), periodOf(item, column.getPeriod()),
                        document.getPhamVi().getValue(), value, document.getTaiLieuId(), page, form,
                        row.getCode(), column.getPeriod(), column.isCurrent(), column.isRestated()));
            }
        }
    }

    // số dư (tiền đầu kỳ, cuối kỳ) nằm trong báo cáo thời kỳ B03 thì quy về đúng một ngày
    private static Period periodOf(FinancialCatalog.Item item, Period column) {
        if (item.getLoaiSo() != LoaiSo.THOI_DIEM || column.isPoint()) {
            return column;
        }
        if ("TIEN_DAU_KY".equals(item.getMaNoiBo())) {
            return Period.point(column.getTuNgay().minusDays(1));
        }
        return Period.point(column.getDenNgay());
    }

    // trong một tài liệu, mỗi (mẫu biểu, kỳ của cột) là một nhóm: trùng mã thì phải trùng số,
    // rồi kiểm các phép cộng; nhóm nào trượt thì loại các số tham gia
    private void checkConsistency(List<FigureCandidate> read) {
        Map<String, Map<String, FigureCandidate>> groups = new LinkedHashMap<>();
        Map<String, Set<String>> conflicted = new HashMap<>();
        for (FigureCandidate c : read) {
            String group = c.getMauBieu() + "|" + c.getColumnPeriod();
            Map<String, FigureCandidate> byCode = groups.computeIfAbsent(group, k -> new LinkedHashMap<>());
            Set<String> bad = conflicted.computeIfAbsent(group, k -> new HashSet<>());
            if (bad.contains(c.getMaNoiBo())) {
                continue;
            }
            FigureCandidate seen = byCode.get(c.getMaNoiBo());
            if (seen == null) {
                byCode.put(c.getMaNoiBo(), c);
            } else if (seen.getGiaTri() != c.getGiaTri()) {
                // cùng tài liệu, cùng mã, cùng kỳ mà hai số khác nhau: không biết số nào đúng
                byCode.remove(c.getMaNoiBo());
                bad.add(c.getMaNoiBo());
                issue(c.getTrang(), LoaiLoi.TRUNG_MA_KHAC_SO, c.getMauBieu(), c.getMa(), null, null,
                        String.valueOf(c.getGiaTri()),
                        "trang " + seen.getTrang() + " đọc " + seen.getGiaTri() + ", cột " + c.getColumnPeriod());
            }
        }

        for (Map.Entry<String, Map<String, FigureCandidate>> entry : groups.entrySet()) {
            Map<String, FigureCandidate> byCode = entry.getValue();
            if (byCode.isEmpty()) {
                continue;
            }
            MauBieu form = byCode.values().iterator().next().getMauBieu();
            Map<String, Long> values = new HashMap<>();
            byCode.forEach((code, c) -> values.put(code, c.getGiaTri()));

            Set<String> rejected = new HashSet<>();
            for (BalanceIdentities.Failure f : BalanceIdentities.check(form, values)) {
                FigureCandidate total = byCode.get(f.getIdentity().getTotal());
                issue(total.getTrang(), LoaiLoi.LECH_CAN_DOI, form, total.getMa(), null, null,
                        String.valueOf(f.getActual()),
                        f.getIdentity() + ": cộng ra " + f.getExpected() + ", lệch " + (f.getActual() - f.getExpected())
                                + ", cột " + total.getColumnPeriod());
                rejected.add(f.getIdentity().getTotal());
                rejected.addAll(f.getIdentity().getParts().keySet());
            }
            byCode.forEach((code, c) -> {
                if (!rejected.contains(code)) {
                    result.candidates.add(c);
                }
            });
        }
    }

    // mẫu biểu theo tên gần bảng nhất; không có tên thì xem tiêu đề cột; vẫn không có thì mang theo bảng trước
    private MauBieu detectForm(String near, String pageText, StatementTable table) {
        String s = VietnameseText.fold(near);
        int b01 = Math.max(s.lastIndexOf("can doi ke toan"), s.lastIndexOf("tinh hinh tai chinh"));
        int b02 = Math.max(s.lastIndexOf("ket qua hoat dong"), s.lastIndexOf("ket qua kinh doanh"));
        int b03 = s.lastIndexOf("luu chuyen tien te");
        MauBieu form;
        if (b03 >= 0 && b03 >= b01 && b03 >= b02) {
            form = cashFlowForm(pageText, table);
        } else if (b02 >= 0 && b02 >= b01) {
            form = MauBieu.B02;
        } else if (b01 >= 0) {
            form = MauBieu.B01;
        } else if (s.contains("tai san") || s.contains("nguon von")) {
            form = MauBieu.B01;
        } else {
            form = lastForm;
        }
        if (form != null) {
            lastForm = form;
            if (form.isCashFlow()) {
                lastCashFlowForm = form;
            }
        }
        return form;
    }

    // B03 trực tiếp hay gián tiếp: xem chữ trên trang, rồi bảng B03 trước, cuối cùng cho các dòng bỏ phiếu
    private MauBieu cashFlowForm(String pageText, StatementTable table) {
        String s = VietnameseText.fold(pageText);
        if (s.contains("gian tiep")) {
            return MauBieu.B03_GT;
        }
        if (s.contains("truc tiep")) {
            return MauBieu.B03_TT;
        }
        return lastCashFlowForm != null ? lastCashFlowForm : voteCashFlow(table);
    }

    // kiểm chéo bằng mã: B01 dùng mã 3 chữ số (100, 270), B02 và B03 dùng mã 2 chữ số (01, 60).
    // tên mẫu biểu và mã nói khác nhau thì tin mã, rồi cho các dòng bỏ phiếu theo tên
    private MauBieu checkByCodes(MauBieu detected, StatementTable table) {
        long threeDigit = table.getRows().stream().filter(r -> r.getCode().length() >= 3).count();
        double share = (double) threeDigit / table.getRows().size();
        MauBieu form = detected;
        if (share >= 0.6) {
            form = MauBieu.B01;
        } else if (share <= 0.2 && (detected == null || detected == MauBieu.B01)) {
            form = vote(table, MauBieu.B02, MauBieu.B03_GT, MauBieu.B03_TT);
        }
        if (form != null && form != detected) {
            lastForm = form;
            if (form.isCashFlow()) {
                lastCashFlowForm = form;
            }
        }
        return form;
    }

    // không biết phương pháp: chọn mẫu có nhiều dòng khớp tên hơn
    private MauBieu voteCashFlow(StatementTable table) {
        return vote(table, MauBieu.B03_GT, MauBieu.B03_TT);
    }

    // tỉ lệ dòng có (mã, tên) khớp bảng ánh xạ của một mẫu biểu
    private double matchShare(StatementTable table, MauBieu form) {
        long matched = table.getRows().stream().filter(row -> {
            FinancialCatalog.Mapping m = catalog.mapping(cheDo, form, row.getCode());
            return m != null && NameMatcher.matches(row.getName(), m.getTenTrenBaoCao());
        }).count();
        return (double) matched / table.getRows().size();
    }

    // mẫu biểu có nhiều dòng (mã, tên) khớp bảng ánh xạ nhất; không dòng nào khớp thì null
    private MauBieu vote(StatementTable table, MauBieu... forms) {
        MauBieu best = null;
        int bestCount = 0;
        for (MauBieu form : forms) {
            int count = 0;
            for (StatementTable.Row row : table.getRows()) {
                FinancialCatalog.Mapping m = catalog.mapping(cheDo, form, row.getCode());
                if (m != null && NameMatcher.matches(row.getName(), m.getTenTrenBaoCao())) {
                    count++;
                }
            }
            if (count > bestCount) {
                best = form;
                bestCount = count;
            }
        }
        return best;
    }

    // vài dòng đầu của bảng gốc (kể cả dòng không có mã): có trang ghi tên mẫu biểu ngay trong bảng
    private static String leadText(com.vdt.finq.common.markdown.MarkdownTable table) {
        StringBuilder s = new StringBuilder();
        table.getBodyRows().stream().limit(6).forEach(row -> s.append(String.join(" ", row)).append('\n'));
        return s.toString();
    }

    // đơn vị ghi ở tiêu đề bảng; mặc định đồng
    private static long unit(String text) {
        String s = VietnameseText.fold(text);
        if (s.contains("trieu dong")) {
            return 1_000_000L;
        }
        if (s.contains("nghin dong") || s.contains("ngan dong")) {
            return 1_000L;
        }
        return 1L;
    }

    private static String headerText(StatementTable table) {
        StringBuilder s = new StringBuilder();
        for (List<String> row : table.getHeaderRows()) {
            s.append(String.join(" ", row)).append('\n');
        }
        return s.toString();
    }

    // vài dòng đầu của bảng: có trang ghi tên mẫu biểu ngay trong bảng
    private static String firstNames(StatementTable table) {
        StringBuilder s = new StringBuilder();
        table.getRows().stream().limit(3).forEach(r -> s.append(r.getName()).append('\n'));
        return s.toString();
    }

    private static String first(StatementTable.Row row) {
        return row.getValues().isEmpty() ? null : row.getValues().get(0);
    }

    private void issue(int page, LoaiLoi loai, MauBieu form, String ma, String tenDoc, String tenKyVong,
            String giaTri, String chiTiet) {
        result.issues.add(new ExtractionIssue(document.getTaiLieuId(), page, loai, cheDo,
                form == null ? null : form.getValue(), ma, tenDoc, tenKyVong, giaTri, chiTiet));
    }
}
