package com.vdt.finq.common.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// đọc bảng markdown trong một đoạn văn bản; dùng cho trích số và table-to-json
public final class MarkdownTableParser {

    // ô của dòng kẻ dưới tiêu đề: ---, :---, ---:, :---:
    private static final Pattern SEPARATOR_CELL = Pattern.compile(":?-{2,}:?");

    // đánh dấu đậm, nghiêng của markdown quanh nội dung ô
    private static final Pattern EMPHASIS = Pattern.compile("\\*\\*|__|(?<!\\w)\\*|\\*(?!\\w)");

    private MarkdownTableParser() {
    }

    // một khối của văn bản: hoặc là chữ thường (table = null), hoặc là một bảng
    public static final class Block {
        private final String text;
        private final MarkdownTable table;

        private Block(String text, MarkdownTable table) {
            this.text = text;
            this.table = table;
        }

        public String getText() {
            return text;
        }

        public MarkdownTable getTable() {
            return table;
        }

        public boolean isTable() {
            return table != null;
        }
    }

    // chia văn bản thành các khối chữ và bảng, giữ đúng thứ tự
    public static List<Block> split(String text) {
        List<Block> blocks = new ArrayList<>();
        List<String> textLines = new ArrayList<>();
        List<String> tableLines = new ArrayList<>();

        for (String line : text.split("\n", -1)) {
            if (isTableLine(line)) {
                if (!textLines.isEmpty()) {
                    blocks.add(new Block(String.join("\n", textLines), null));
                    textLines.clear();
                }
                tableLines.add(line);
            } else {
                if (!tableLines.isEmpty()) {
                    blocks.add(new Block(null, toTable(tableLines)));
                    tableLines.clear();
                }
                textLines.add(line);
            }
        }
        if (!tableLines.isEmpty()) {
            blocks.add(new Block(null, toTable(tableLines)));
        }
        if (!textLines.isEmpty()) {
            blocks.add(new Block(String.join("\n", textLines), null));
        }
        return blocks;
    }

    // chỉ lấy các bảng
    public static List<MarkdownTable> tables(String text) {
        return split(text).stream().filter(Block::isTable).map(Block::getTable).toList();
    }

    public static boolean isTableLine(String line) {
        return line.stripLeading().startsWith("|");
    }

    // "| a | **b** |" -> ["a", "b"]; bỏ ký hiệu đậm nghiêng và khoảng trắng thừa
    public static List<String> cells(String line) {
        String s = line.strip();
        if (s.startsWith("|")) {
            s = s.substring(1);
        }
        if (s.endsWith("|")) {
            s = s.substring(0, s.length() - 1);
        }
        List<String> cells = new ArrayList<>();
        for (String raw : s.split("\\|", -1)) {
            cells.add(EMPHASIS.matcher(raw).replaceAll("").replace("<br>", " ").strip());
        }
        return cells;
    }

    public static boolean isSeparator(List<String> cells) {
        boolean any = false;
        for (String cell : cells) {
            if (cell.isEmpty()) {
                continue;
            }
            if (!SEPARATOR_CELL.matcher(cell.replace(" ", "")).matches()) {
                return false;
            }
            any = true;
        }
        return any;
    }

    // dòng kẻ đầu tiên chia tiêu đề và thân; các dòng kẻ lặp lại về sau (do gối tiêu đề) thì bỏ
    private static MarkdownTable toTable(List<String> lines) {
        List<List<String>> header = new ArrayList<>();
        List<List<String>> body = new ArrayList<>();
        boolean seenSeparator = false;
        for (String line : lines) {
            List<String> row = cells(line);
            if (isSeparator(row)) {
                if (!seenSeparator) {
                    header.addAll(body);
                    body.clear();
                    seenSeparator = true;
                }
                continue;
            }
            body.add(row);
        }
        return new MarkdownTable(List.copyOf(header), List.copyOf(body), List.copyOf(lines));
    }
}
