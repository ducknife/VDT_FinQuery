package com.vdt.finq.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.markdown.MarkdownTable;
import com.vdt.finq.common.markdown.MarkdownTableParser;
import com.vdt.finq.common.markdown.MarkdownTableParser.Block;

import lombok.RequiredArgsConstructor;

// đổi bảng markdown trong một chunk sang JSON, mỗi dòng bảng thành một object {tên cột: giá trị}.
// theo paper Hierarchical Reranker (Lee & Hong, arXiv 2607.27523): làm bằng quy tắc, không dùng LLM,
// để con số luôn đi kèm tiêu đề cột của nó khi embed. chữ ngoài bảng giữ nguyên.
// giá trị giữ đúng chuỗi gốc ("5.627.168.242.619"), không đổi sang số
@Component
@RequiredArgsConstructor
public class TableToJson {

    // số có nhóm hàng nghìn: dấu hiệu của dòng dữ liệu, không phải dòng tiêu đề
    private static final Pattern GROUPED_NUMBER = Pattern.compile("\\(?-?\\d{1,3}(\\.\\d{3})+\\)?");

    private final ObjectMapper objectMapper;

    public String convert(String text) {
        if (text == null || !text.contains("|")) {
            return text;
        }
        List<String> parts = new ArrayList<>();
        for (Block block : MarkdownTableParser.split(text)) {
            parts.add(block.isTable() ? tableToJson(block.getTable()) : block.getText());
        }
        return String.join("\n", parts);
    }

    // bảng không có dòng tiêu đề thì không biết đặt tên khoá: giữ nguyên markdown
    private String tableToJson(MarkdownTable table) {
        if (!table.hasHeader()) {
            return String.join("\n", table.getLines());
        }
        List<List<String>> header = new ArrayList<>(table.getHeaderRows());
        List<List<String>> body = table.getBodyRows();

        // tiêu đề nhiều tầng: các dòng ngay sau dòng kẻ chỉ ghi kỳ, đơn vị (vd "| | | Quý 4/2019 | VND |")
        int start = 0;
        while (start < body.size() && isHeaderContinuation(body.get(start))) {
            header.add(body.get(start++));
        }

        int width = header.stream().mapToInt(List::size).max().orElse(0);
        for (int i = start; i < body.size(); i++) {
            width = Math.max(width, body.get(i).size());
        }
        List<String> keys = keys(header, width);

        List<String> rows = new ArrayList<>();
        for (int i = start; i < body.size(); i++) {
            Map<String, String> object = new LinkedHashMap<>();
            List<String> row = body.get(i);
            for (int c = 0; c < row.size(); c++) {
                if (!row.get(c).isEmpty()) {
                    object.put(keys.get(c), row.get(c));
                }
            }
            if (!object.isEmpty()) {
                rows.add(write(object));
            }
        }
        return "[\n" + String.join(",\n", rows) + "\n]";
    }

    // tên khoá của mỗi cột = nối các tầng tiêu đề. ô trống ở tầng trên là ô gộp (colspan)
    // nên mượn chữ của ô bên trái, vd "Số liệu lũy kế" phủ cả hai cột năm nay, năm trước
    private static List<String> keys(List<List<String>> header, int width) {
        List<Set<String>> parts = new ArrayList<>();
        for (int c = 0; c < width; c++) {
            parts.add(new LinkedHashSet<>());
        }
        for (int r = 0; r < header.size(); r++) {
            List<String> row = header.get(r);
            boolean upper = r < header.size() - 1;
            String carry = "";
            for (int c = 0; c < width; c++) {
                String cell = c < row.size() ? row.get(c) : "";
                if (!cell.isEmpty()) {
                    carry = cell;
                } else if (upper && c >= 2) {
                    cell = carry;
                }
                if (!cell.isEmpty()) {
                    parts.get(c).add(cell);
                }
            }
        }
        List<String> keys = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        for (int c = 0; c < width; c++) {
            String key = parts.get(c).isEmpty() ? "cot_" + (c + 1) : String.join(" - ", parts.get(c));
            // hai cột trùng tên thì đánh số để không mất giá trị
            String unique = key;
            for (int n = 2; used.contains(unique); n++) {
                unique = key + " #" + n;
            }
            used.add(unique);
            keys.add(unique);
        }
        return keys;
    }

    // dòng chỉ có chữ ở các cột giá trị (từ cột thứ 3), không có số nhóm nghìn
    private static boolean isHeaderContinuation(List<String> row) {
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

    private String write(Map<String, String> object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            // map chuỗi -> chuỗi thì không thể lỗi
            throw new IllegalStateException(e);
        }
    }
}
