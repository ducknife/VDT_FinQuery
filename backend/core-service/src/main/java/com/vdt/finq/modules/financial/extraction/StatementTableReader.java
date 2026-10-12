package com.vdt.finq.modules.financial.extraction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vdt.finq.common.markdown.MarkdownTable;
import com.vdt.finq.common.text.VietnameseText;

// nhận ra một bảng markdown có phải bảng báo cáo tài chính (có cột "Mã số") không,
// và cột nào là mã, tên, giá trị. dựa vào DỮ LIỆU của các dòng chứ không chỉ dòng tiêu đề,
// vì OCR hay gộp ô tiêu đề ("Mã số CHỈ TIÊU") làm tiêu đề lệch cột so với thân bảng.
//
// giá trị của mỗi dòng là N ô CUỐI CÙNG (N = số cột giá trị): cột số luôn ở bên phải cùng của mẫu biểu,
// còn ô thuyết minh lúc có lúc không làm các dòng lệch nhau một ô
public final class StatementTableReader {

    // mã số trên báo cáo: 01, 10, 100, 411a
    private static final Pattern CODE = Pattern.compile("\\d{1,3}[a-z]?");
    private static final Pattern GROUPED_NUMBER = Pattern.compile("\\d{1,3}(\\.\\d{3})+");
    private static final Pattern LETTERS = Pattern.compile("\\p{L}{3,}");
    // mã dính đầu ô tên: "110 I. Tiền và các khoản tương đương tiền"
    private static final Pattern EMBEDDED = Pattern.compile("(\\d{1,3}[a-z]?)\\s+(\\S.*\\p{L}.*)");

    private StatementTableReader() {
    }

    // null nếu không phải bảng báo cáo có đúng 2 hoặc 4 cột giá trị
    public static StatementTable read(MarkdownTable table) {
        List<List<String>> header = new ArrayList<>(table.getHeaderRows());
        List<List<String>> body = table.getBodyRows();
        int start = 0;
        while (start < body.size() && isHeaderContinuation(body.get(start))) {
            header.add(body.get(start++));
        }
        int labelColumn = codeLabelColumn(header);
        if (labelColumn < 0 || isReconciliation(header)) {
            return null;
        }
        List<List<String>> data = body.subList(start, body.size());
        Layout layout = layout(data, labelColumn);
        if (layout == null) {
            return null;
        }
        int n = valueColumns(data, layout).size();
        // bảng đối chiếu trình bày lại có 3 cột (năm trước, điều chỉnh, chênh lệch): không phải mẫu biểu
        if (n != 2 && n != 4) {
            return null;
        }
        return build(header, data, layout, n);
    }

    // bảng tiếp theo trang trước mà mất dòng tiêu đề (chỉ còn "| | | |"): đọc với số cột của bảng trước.
    // số cột giá trị đếm trên chính trang này phải BẰNG số cột của bảng trước: OCR làm rơi một cột
    // (BCTC HN Q3/2024 trang 6 chỉ còn 3 cột) thì N ô cuối sẽ lấy cả ô thuyết minh trống và lệch
    // nguyên dòng sang một kỳ khác. không khớp thì không đọc, không đoán cột nào bị rơi
    public static StatementTable readContinuation(MarkdownTable table, int n) {
        List<List<String>> data = new ArrayList<>(table.getHeaderRows());
        data.addAll(table.getBodyRows());
        Layout layout = layout(data, 0);
        if (layout == null || valueColumns(data, layout).isEmpty() || dominantRowValueCount(data, layout) != n) {
            return null;
        }
        return build(List.of(), data, layout, n);
    }

    // số ô giá trị phổ biến nhất trên các dòng có mã. đếm theo TỪNG DÒNG chứ không theo vị trí cột,
    // vì dòng có ô thuyết minh, dòng không làm vị trí cột lệch nhau. ô giá trị = số có nhóm nghìn hoặc "-"
    static int dominantRowValueCount(List<List<String>> data, Layout layout) {
        java.util.Map<Integer, Integer> freq = new java.util.HashMap<>();
        for (List<String> row : data) {
            String code = cell(row, layout.codeColumn);
            if (layout.embedded ? !EMBEDDED.matcher(code).matches() : normalizeCode(code) == null) {
                continue;
            }
            int count = 0;
            for (int c = Math.max(layout.codeColumn, layout.nameColumn) + 1; c < row.size(); c++) {
                String v = row.get(c).strip();
                if (GROUPED_NUMBER.matcher(v).find() || v.equals("-") || v.equals("–")) {
                    count++;
                }
            }
            if (count > 0) {
                freq.merge(count, 1, Integer::sum);
            }
        }
        return freq.entrySet().stream().max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey).orElse(0);
    }

    // vị trí cột mã, cột tên, và mã có dính trong ô tên không
    private static final class Layout {
        private final int codeColumn;
        private final int nameColumn;
        private final boolean embedded;

        private Layout(int codeColumn, int nameColumn, boolean embedded) {
            this.codeColumn = codeColumn;
            this.nameColumn = nameColumn;
            this.embedded = embedded;
        }
    }

    private static Layout layout(List<List<String>> data, int labelColumn) {
        int plain = best(data, labelColumn, false);
        int embedded = best(data, labelColumn, true);
        if (embedded >= 0 && count(data, embedded, true) > (plain < 0 ? 0 : count(data, plain, false))) {
            return new Layout(embedded, embedded, true);
        }
        if (plain < 0) {
            return null;
        }
        return new Layout(plain, nameColumn(data, plain), false);
    }

    private static StatementTable build(List<List<String>> header, List<List<String>> data, Layout layout, int n) {
        List<StatementTable.Row> rows = new ArrayList<>();
        // mã 3 chữ số gần nhất, để suy mã cho dòng "- Nguyên giá", "- Giá trị hao mòn lũy kế" không ghi mã
        int parent = -1;
        for (List<String> row : data) {
            String code;
            String name;
            if (layout.embedded) {
                Matcher m = EMBEDDED.matcher(cell(row, layout.codeColumn));
                if (!m.matches()) {
                    continue;
                }
                code = normalizeCode(m.group(1));
                name = m.group(2);
            } else {
                code = normalizeCode(cell(row, layout.codeColumn));
                name = layout.nameColumn < 0 ? "" : cell(row, layout.nameColumn);
                if (code == null) {
                    code = childCode(parent, name);
                }
            }
            if (code == null) {
                continue;
            }
            if (code.length() == 3 && Character.isDigit(code.charAt(2)) && childOffset(name) == 0) {
                parent = Integer.parseInt(code);
            }
            List<String> values = trailingValues(row, Math.max(layout.codeColumn, layout.nameColumn), n);
            if (values != null) {
                rows.add(new StatementTable.Row(code, name, values));
            }
        }
        if (rows.isEmpty()) {
            return null;
        }
        return new StatementTable(List.copyOf(header), layout.codeColumn, n, rows);
    }

    // n ô cuối của dòng, sau cột tên; thiếu ô thì null (không đoán ô nào bị rơi)
    private static List<String> trailingValues(List<String> row, int lastLabelColumn, int n) {
        List<String> after = row.subList(Math.min(row.size(), lastLabelColumn + 1), row.size());
        if (after.size() < n) {
            return null;
        }
        return List.copyOf(after.subList(after.size() - n, after.size()));
    }

    // "1" -> "01" (mã B02, B03 luôn hai chữ số); không phải mã thì null
    static String normalizeCode(String raw) {
        String s = raw.strip().toLowerCase();
        if (!CODE.matcher(s).matches()) {
            return null;
        }
        return s.length() == 1 ? "0" + s : s;
    }

    // B01 xếp mã con ngay sau mã cha: 221 TSCĐ hữu hình -> 222 nguyên giá, 223 hao mòn (cả TT200 và TT99).
    // nhiều báo cáo bỏ trống mã của hai dòng con này; suy lại từ dòng cha gần nhất
    static String childCode(int parent, String name) {
        int offset = childOffset(name);
        return parent > 0 && offset > 0 ? String.valueOf(parent + offset) : null;
    }

    // 1: dòng nguyên giá, 2: dòng hao mòn lũy kế, 0: không phải dòng con
    private static int childOffset(String name) {
        String s = VietnameseText.fold(name);
        if (s.matches("^[-+ ]*nguyen gia.*")) {
            return 1;
        }
        if (s.matches("^[-+ ]*(gia tri )?hao mon luy ke.*")) {
            return 2;
        }
        return 0;
    }

    // dòng chỉ ghi kỳ, đơn vị ở các cột giá trị (từ cột thứ 3), không có số nhóm nghìn
    static boolean isHeaderContinuation(List<String> row) {
        boolean any = false;
        for (int c = 0; c < row.size(); c++) {
            String cell = row.get(c);
            if (cell.isEmpty()) {
                continue;
            }
            if (c < 2 || GROUPED_NUMBER.matcher(cell).find()) {
                return false;
            }
            any = true;
        }
        return any;
    }

    // bảng đối chiếu trong thuyết minh ("Phân loại lại | Đã trình bày trên báo cáo năm trước",
    // "... | Chênh lệch"): có cột mã số nhưng không phải số của kỳ nào cả.
    // riêng "đã điều chỉnh" thì KHÔNG loại: đó là cột so sánh đã trình bày lại của chính mẫu biểu
    static boolean isReconciliation(List<List<String>> header) {
        for (List<String> row : header) {
            for (String cell : row) {
                String s = VietnameseText.fold(cell);
                if (s.contains("phan loai lai") || s.contains("da trinh bay tren") || s.contains("chenh lech")
                        || s.contains("so lieu tren bao cao")) {
                    return true;
                }
            }
        }
        return false;
    }

    // ô tiêu đề nào ghi "Mã số" (kể cả ô gộp "Mã số CHỈ TIÊU")
    private static int codeLabelColumn(List<List<String>> header) {
        for (List<String> row : header) {
            for (int c = 0; c < row.size(); c++) {
                String s = VietnameseText.fold(row.get(c));
                if (s.startsWith("ma so") || s.contains(" ma so") || s.startsWith("ma ")) {
                    return c;
                }
            }
        }
        return -1;
    }

    // cột mã thật là cột (quanh vị trí tiêu đề) có nhiều ô dạng mã nhất
    private static int best(List<List<String>> data, int labelColumn, boolean embedded) {
        int best = -1;
        int bestCount = 0;
        for (int c = Math.max(0, labelColumn - 1); c <= labelColumn + 1; c++) {
            int count = count(data, c, embedded);
            if (count > bestCount) {
                best = c;
                bestCount = count;
            }
        }
        return bestCount >= 2 ? best : -1;
    }

    private static int count(List<List<String>> data, int column, boolean embedded) {
        int count = 0;
        for (List<String> row : data) {
            String cell = cell(row, column);
            if (embedded ? EMBEDDED.matcher(cell).matches() : normalizeCode(cell) != null) {
                count++;
            }
        }
        return count;
    }

    // cột tên nằm sát cột mã, bên nào nhiều chữ hơn
    private static int nameColumn(List<List<String>> data, int codeColumn) {
        int right = letterCount(data, codeColumn + 1);
        int left = codeColumn > 0 ? letterCount(data, codeColumn - 1) : -1;
        if (right <= 0 && left <= 0) {
            return -1;
        }
        return right >= left ? codeColumn + 1 : codeColumn - 1;
    }

    // cột giá trị: có ít nhất một số nhóm nghìn và hầu hết ô có chữ là số hoặc gạch ngang.
    // cột thuyết minh ("3", "16", "V.1") không có số nhóm nghìn nên tự bị loại.
    // chỉ dùng để ĐẾM số cột giá trị; giá trị của từng dòng lấy theo trailingValues
    private static List<Integer> valueColumns(List<List<String>> data, Layout layout) {
        int width = data.stream().mapToInt(List::size).max().orElse(0);
        List<Integer> columns = new ArrayList<>();
        for (int c = 0; c < width; c++) {
            if (c == layout.codeColumn || c == layout.nameColumn) {
                continue;
            }
            int grouped = 0;
            int filled = 0;
            int numeric = 0;
            for (List<String> row : data) {
                String code = cell(row, layout.codeColumn);
                if (layout.embedded ? !EMBEDDED.matcher(code).matches() : normalizeCode(code) == null) {
                    continue;
                }
                String v = cell(row, c);
                if (v.isEmpty()) {
                    continue;
                }
                filled++;
                if (GROUPED_NUMBER.matcher(v).find()) {
                    grouped++;
                }
                if (VietnameseNumberParser.looksNumeric(v)) {
                    numeric++;
                }
            }
            if (grouped > 0 && numeric >= 0.8 * filled) {
                columns.add(c);
            }
        }
        return columns;
    }

    private static int letterCount(List<List<String>> data, int column) {
        int count = 0;
        for (List<String> row : data) {
            if (LETTERS.matcher(cell(row, column)).find()) {
                count++;
            }
        }
        return count;
    }

    private static String cell(List<String> row, int column) {
        return column >= 0 && column < row.size() ? row.get(column) : "";
    }
}
