package com.vdt.finq.modules.financial.extraction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vdt.finq.common.text.VietnameseText;

import lombok.AllArgsConstructor;
import lombok.Getter;

// gán kỳ cho từng cột giá trị: kỳ của tài liệu quyết định phương án, tiêu đề cột chỉ để chọn
// và xác nhận; tiêu đề mâu thuẫn thì từ chối, không đoán
public final class ColumnPlanner {

    private static final Pattern HEADING_RANGE = Pattern.compile(
            "tu ngay (\\d{1,2})/(\\d{1,2})/(\\d{4}) den ngay (\\d{1,2})/(\\d{1,2})/(\\d{4})");

    private ColumnPlanner() {
    }

    @Getter
    @AllArgsConstructor
    public static class Column {
        private final Period period;
        // cột kỳ này của tài liệu (cot_ky_nay) hay cột so sánh (cot_ky_truoc)
        private final boolean current;
        // số đã trình bày lại ("đã điều chỉnh")
        private final boolean restated;
    }

    // kết quả: columns khác null nếu gán được, ngược lại reason ghi lý do
    @Getter
    @AllArgsConstructor
    public static class Plan {
        private final List<Column> columns;
        private final String reason;
    }

    public static Plan plan(boolean balanceSheet, LocalDate documentEnd, StatementTable table, String headingText) {
        int n = table.getValueCount();
        List<List<Period>> options = options(balanceSheet, documentEnd, n);
        if (options.isEmpty()) {
            return new Plan(null, "không có phương án cho " + n + " cột");
        }

        // 1. tiêu đề thẳng cột: đọc kỳ của từng cột giá trị
        List<PeriodLabel> aligned = alignedLabels(table);
        List<Period> chosen = matchAligned(options, aligned);

        // 2. tiêu đề lệch cột (OCR gộp, đảo ô): so cả tập kỳ đọc được với từng phương án
        boolean jumbled = false;
        if (chosen == null) {
            chosen = matchAsSet(options, allLabels(table));
            jumbled = chosen != null;
        }

        // 3. tiêu đề không ghi kỳ ("Kỳ này", "Số cuối kỳ"): dùng dòng "Từ ngày ... đến ngày ..." của trang
        boolean anyResolved = aligned.stream().anyMatch(l -> l.getPeriod() != null)
                || allLabels(table).stream().anyMatch(l -> l.getPeriod() != null);
        if (chosen == null && !anyResolved) {
            chosen = options.size() == 1 ? options.get(0) : byHeading(options, headingText);
        }
        if (chosen == null) {
            return new Plan(null, "tiêu đề cột " + describe(aligned) + " không khớp kỳ của tài liệu (" + documentEnd + ")");
        }

        boolean anyRestated = allLabels(table).stream().anyMatch(PeriodLabel::isRestated);
        List<Column> columns = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Period p = chosen.get(i);
            boolean current = p.getDenNgay().equals(documentEnd);
            boolean restated = jumbled || aligned.size() != n
                    ? anyRestated && !current
                    : aligned.get(i).isRestated();
            columns.add(new Column(p, current, restated));
        }
        return new Plan(columns, null);
    }

    static List<List<Period>> options(boolean balanceSheet, LocalDate end, int n) {
        List<List<Period>> options = new ArrayList<>();
        if (balanceSheet) {
            if (n == 2) {
                options.add(List.of(Period.point(end), Period.point(LocalDate.of(end.getYear() - 1, 12, 31))));
            }
            return options;
        }
        Period quarter = Period.quarterOf(end);
        Period ytd = Period.yearToDate(end);
        if (n == 4) {
            options.add(List.of(quarter, quarter.previousYear(), ytd, ytd.previousYear()));
        } else if (n == 2) {
            options.add(List.of(ytd, ytd.previousYear()));
            if (!quarter.equals(ytd)) {
                options.add(List.of(quarter, quarter.previousYear()));
            }
        }
        return options;
    }

    // nhãn cột = n ô cuối của mỗi dòng tiêu đề, ghép các tầng; ô gộp trống mượn ô bên trái
    private static List<PeriodLabel> alignedLabels(StatementTable table) {
        int n = table.getValueCount();
        List<List<String>> header = table.getHeaderRows();
        List<StringBuilder> texts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            texts.add(new StringBuilder());
        }
        for (int r = 0; r < header.size(); r++) {
            List<String> row = header.get(r);
            if (row.size() < n + 1) {
                continue;
            }
            List<String> tail = row.subList(row.size() - n, row.size());
            boolean upper = r < header.size() - 1;
            String carry = "";
            for (int i = 0; i < n; i++) {
                String cell = tail.get(i);
                if (!cell.isEmpty()) {
                    carry = cell;
                } else if (upper) {
                    cell = carry;
                }
                texts.get(i).append(' ').append(cell);
            }
        }
        List<PeriodLabel> labels = new ArrayList<>();
        for (StringBuilder text : texts) {
            labels.add(PeriodLabel.parse(text.toString()));
        }
        return labels;
    }

    // mọi ô tiêu đề từ cột thứ 3 trở đi, đọc riêng từng ô; ô "lũy kế" phủ các ô cùng hàng bên phải
    private static List<PeriodLabel> allLabels(StatementTable table) {
        List<PeriodLabel> labels = new ArrayList<>();
        for (List<String> row : table.getHeaderRows()) {
            String group = "";
            for (int c = 2; c < row.size(); c++) {
                String cell = row.get(c);
                if (cell.isEmpty()) {
                    continue;
                }
                String folded = VietnameseText.fold(cell);
                if (folded.contains("luy ke") || folded.contains("phat sinh")) {
                    group = cell;
                }
                labels.add(PeriodLabel.parse(group + " " + cell));
            }
        }
        return labels;
    }

    // mọi cột đọc được kỳ đều phải trùng phương án ở đúng vị trí
    private static List<Period> matchAligned(List<List<Period>> options, List<PeriodLabel> aligned) {
        if (aligned.stream().allMatch(l -> l.getPeriod() == null)) {
            return null;
        }
        List<List<Period>> fits = new ArrayList<>();
        for (List<Period> option : options) {
            boolean ok = true;
            for (int i = 0; i < option.size() && ok; i++) {
                ok = compatible(aligned.get(i).getPeriod(), option.get(i));
            }
            if (ok) {
                fits.add(option);
            }
        }
        return fits.size() == 1 ? fits.get(0) : null;
    }

    // nới một ca: cột luỹ kế mà tiêu đề chỉ ghi "Quý 2/2024" (OCR rơi dòng "Số liệu lũy kế"), cùng ngày cuối thì nhận
    private static boolean compatible(Period read, Period expected) {
        if (read == null || read.equals(expected)) {
            return true;
        }
        boolean expectedYtd = expected.getTuNgay().getDayOfYear() == 1 && !expected.isPoint();
        return expectedYtd && !read.isPoint() && read.getDenNgay().equals(expected.getDenNgay())
                && read.equals(Period.quarterOf(read.getDenNgay()));
    }

    // tập kỳ đọc được (bỏ trùng) phải đúng bằng tập kỳ của một phương án
    private static List<Period> matchAsSet(List<List<Period>> options, List<PeriodLabel> labels) {
        Set<Period> read = new LinkedHashSet<>();
        for (PeriodLabel label : labels) {
            if (label.getPeriod() != null) {
                read.add(label.getPeriod());
            }
        }
        if (read.isEmpty()) {
            return null;
        }
        List<Period> found = null;
        for (List<Period> option : options) {
            if (new LinkedHashSet<>(option).equals(read)) {
                if (found != null) {
                    return null;
                }
                found = option;
            }
        }
        return found;
    }

    // tiêu đề trang "Cho kỳ kế toán từ ngày 01/01/2026 đến ngày 30/06/2026" chọn giữa quý và lũy kế
    private static List<Period> byHeading(List<List<Period>> options, String headingText) {
        Matcher m = HEADING_RANGE.matcher(VietnameseText.fold(headingText));
        if (!m.find()) {
            return null;
        }
        Period heading;
        try {
            heading = Period.range(
                    LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1))),
                    LocalDate.of(Integer.parseInt(m.group(6)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(4))));
        } catch (java.time.DateTimeException e) {
            return null;
        }
        for (List<Period> option : options) {
            if (option.get(0).equals(heading)) {
                return option;
            }
        }
        return null;
    }

    private static String describe(List<PeriodLabel> labels) {
        Map<Integer, String> shown = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            shown.put(i, Objects.toString(labels.get(i).getPeriod(), "?"));
        }
        return shown.values().toString();
    }
}
