package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.stitcher;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

// bảng vắt sang trang sau mà OCR coi dòng số đầu là tiêu đề: chép tiêu đề thật của trang trước sang.
// chỉ sửa khi chắc cùng bảng (cùng số cột, trang sau mở đầu bằng bảng, dòng đầu chủ yếu là số)
@Component
public class TableStitcher {

    // dòng chỉ có số trang: "12", "- 12 -"
    private static final Pattern PAGE_NUMBER = Pattern.compile("\\s*-?\\s*\\d{1,3}\\s*-?\\s*");

    // dòng kẻ dưới tiêu đề bảng: |---|---|
    private static final Pattern TABLE_SEPARATOR = Pattern.compile("^\\|?\\s*:?-{3,}.*");

    // ô chỉ chứa số: 36.240.869.667, (1.234), 15%, 30
    private static final Pattern NUMERIC_CELL = Pattern.compile("^[\\s(\\-]*[\\d.,]+[\\s)%]*$");

    // sửa cả tài liệu một lượt; mỗi trang so với bản đã sửa của trang trước
    public List<String> repairTableHeaders(List<String> pages) {
        List<String> repaired = new ArrayList<>(pages);
        for (int i = 1; i < repaired.size(); i++) {
            if (!repaired.get(i - 1).isBlank() && !repaired.get(i).isBlank()) {
                repaired.set(i, repairTableHeader(repaired.get(i - 1), repaired.get(i), i));
            }
        }
        return repaired;
    }

    // sửa một cặp trang; previousPageNumber đếm từ 1, dùng cho nhãn
    public String repairTableHeader(String previousPage, String page, int previousPageNumber) {
        List<String> previous = contentLines(previousPage);
        int start = previous.size();
        while (start > 0 && previous.get(start - 1).startsWith("|")) {
            start--;
        }
        // điều kiện 1
        if (previous.size() - start < 2 || !TABLE_SEPARATOR.matcher(previous.get(start + 1)).matches()) {
            return page;
        }
        String header = previous.get(start);
        String separator = previous.get(start + 1);

        // tiêu đề nhiều tầng: các dòng dưới |---| không có số, chép tối đa 2 dòng
        List<String> subHeaders = new ArrayList<>();
        for (int i = start + 2; i < previous.size() && subHeaders.size() < 2; i++) {
            if (!isSubHeader(previous.get(i))) {
                break;
            }
            subHeaders.add(previous.get(i));
        }

        List<String> lines = new ArrayList<>(page.lines().toList());
        int first = 0;
        while (first < lines.size() && isSkippable(lines.get(first))) {
            first++;
        }
        // điều kiện 2
        if (first == lines.size() || !lines.get(first).strip().startsWith("|")) {
            return page;
        }
        String firstRow = lines.get(first).strip();
        // điều kiện 3
        if (cells(firstRow).size() != cells(header).size()) {
            return page;
        }
        boolean hasSeparator = first + 1 < lines.size()
                && TABLE_SEPARATOR.matcher(lines.get(first + 1).strip()).matches();
        // điều kiện 4
        if (hasSeparator && !isMostlyNumeric(firstRow)) {
            return page;
        }

        // dựng lại: nhãn, tiêu đề thật, rồi dòng số liệu trả về đúng vai, bỏ dòng |---| giả
        List<String> repaired = new ArrayList<>(lines.subList(0, first));
        repaired.add("(tiêu đề bảng từ trang " + previousPageNumber + ")");
        repaired.add("");
        repaired.add(header);
        repaired.add(separator);
        repaired.addAll(subHeaders);
        repaired.add(firstRow);
        repaired.addAll(lines.subList(hasSeparator ? first + 2 : first + 1, lines.size()));
        return String.join("\n", repaired);
    }

    // ít nhất một nửa số ô có nội dung là số
    private static boolean isMostlyNumeric(String row) {
        List<String> filled = cells(row).stream().filter(c -> !c.isEmpty()).toList();
        long numeric = filled.stream().filter(c -> NUMERIC_CELL.matcher(c).matches()).count();
        return !filled.isEmpty() && numeric * 2 >= filled.size();
    }

    // dòng tiêu đề phụ: không ô nào là số, ít nhất một nửa số ô có chữ
    private static boolean isSubHeader(String row) {
        List<String> all = cells(row);
        List<String> filled = all.stream().filter(c -> !c.isEmpty()).toList();
        boolean hasNumber = filled.stream().anyMatch(c -> NUMERIC_CELL.matcher(c).matches());
        return !hasNumber && filled.size() * 2 >= all.size();
    }

    // tách một dòng bảng thành các ô
    private static List<String> cells(String row) {
        String inner = row.strip();
        if (inner.startsWith("|")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith("|")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        return List.of(inner.split("\\|", -1)).stream().map(String::strip).toList();
    }

    // các dòng có nội dung, bỏ dòng trống và dòng số trang
    private static List<String> contentLines(String page) {
        return page.lines().map(String::strip).filter(l -> !isSkippable(l)).toList();
    }

    private static boolean isSkippable(String line) {
        String s = line.strip();
        return s.isEmpty() || PAGE_NUMBER.matcher(s).matches();
    }
}
