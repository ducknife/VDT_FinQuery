package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.vdt.finq.common.properties.IndexingProperties;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.stitcher.TableStitcher;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.stitcher.TextStitcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// chunk đệ quy theo cấu trúc, đơn vị gốc là trang: sửa ranh giới trang (bảng, câu), trang <= MAX giữ nguyên,
// lớn hơn thì chia heading -> khối -> dòng/câu rồi gộp mảnh vụn; không gộp xuyên trang.
// chi tiết: .docs/projectdevlogs/lap-chi-muc_v1.6.md
@Slf4j
@Component
@RequiredArgsConstructor
public class Chunking {

    // vị trí ngay trước một dòng heading
    private static final Pattern BEFORE_HEADING = Pattern.compile("(?m)^(?=[ \\t]*#{1,6}\\s)");

    // vị trí ngay sau dấu hết câu và một khoảng trắng, 1.061 không bị tính
    private static final Pattern AFTER_SENTENCE = Pattern.compile("(?<=[.!?][ \\t])");

    // dòng kẻ dưới tiêu đề bảng: |---|---|
    private static final Pattern TABLE_SEPARATOR = Pattern.compile("^\\s*\\|?\\s*:?-{3,}.*");

    private final IndexingProperties properties;
    private final TableStitcher tableStitcher;
    private final TextStitcher textStitcher;

    // chia một tài liệu thành chunk; pages là các trang đã qua TextCleaner, trang 1 ở vị trí 0
    public List<Chunk> chunk(String documentId, List<String> cleanedPages) {
        // sửa tiêu đề bảng vắt qua trang trước, cho cả tài liệu một lượt
        List<String> pages = tableStitcher.repairTableHeaders(cleanedPages);

        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).isBlank()) {
                continue;
            }
            int page = i + 1;
            String text = textStitcher.stitch(pages, i);

            // các phần của trang lần lượt nhận part 0, 1, 2...
            int part = 0;
            for (String piece : split(text, SplitLevel.HEADING, documentId, page)) {
                String clean = piece.strip();
                if (!clean.isEmpty()) {
                    chunks.add(new Chunk(documentId, page, part++, clean));
                }
            }
        }
        return chunks;
    }

    // hàm đệ quy: text cần chia bằng đơn vị của mức level
    private List<String> split(String text, SplitLevel level, String documentId, int page) {
        int max = properties.getMaxChunkChars();

        // đủ nhỏ thì dừng
        if (text.strip().length() <= max) {
            return List.of(text);
        }
        // câu hoặc dòng bảng không chia được nữa: giữ nguyên
        if (level == SplitLevel.ATOM) {
            return keepWhole(text, documentId, page);
        }
        // bảng có cách cắt riêng để giữ tiêu đề cột
        if (level == SplitLevel.LINE && isTable(text)) {
            return splitTable(text, documentId, page);
        }

        List<String> result = new ArrayList<>();
        for (String piece : splitBy(level, text)) {
            result.addAll(split(piece, level.next(), documentId, page));
        }
        return mergeGreedy(result, max);
    }

    // tách text theo đơn vị của mức; nối các mảnh lại sẽ ra đúng text ban đầu
    private static List<String> splitBy(SplitLevel level, String text) {
        return switch (level) {
            case HEADING -> nonEmpty(BEFORE_HEADING.split(text));
            case BLOCK -> splitBlocks(text);
            default -> splitLinesAndSentences(text);
        };
    }

    // mức 2: tách ở dòng trống và ở chỗ chuyển giữa dòng bảng với dòng chữ
    private static List<String> splitBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Boolean currentIsTable = null;

        for (String line : text.split("(?<=\n)")) {
            if (line.isBlank()) {
                // dòng trống đóng khối hiện tại, đi kèm khối đó cho khỏi mất ký tự
                current.append(line);
                if (currentIsTable != null) {
                    blocks.add(current.toString());
                    current.setLength(0);
                    currentIsTable = null;
                }
                continue;
            }
            boolean lineIsTable = line.stripLeading().startsWith("|");
            if (currentIsTable != null && lineIsTable != currentIsTable) {
                blocks.add(current.toString());
                current.setLength(0);
            }
            current.append(line);
            currentIsTable = lineIsTable;
        }
        if (!current.isEmpty()) {
            blocks.add(current.toString());
        }
        return blocks;
    }

    // mức 3 cho văn xuôi: tách theo dòng trước, rồi tách theo câu
    private static List<String> splitLinesAndSentences(String text) {
        List<String> pieces = new ArrayList<>();
        for (String line : text.split("(?<=\n)")) {
            pieces.addAll(nonEmpty(AFTER_SENTENCE.split(line)));
        }
        return pieces;
    }

    // mức 3 cho bảng: cắt theo dòng, phần nào cũng gắn lại tiêu đề cột
    private List<String> splitTable(String table, String documentId, int page) {
        List<String> lines = new ArrayList<>(List.of(table.split("(?<=\n)")));

        // bảng có tiêu đề chuẩn: dòng tiêu đề + dòng |---|
        String header = "";
        // strip trước khi so khớp: dấu . trong regex không khớp ký tự xuống dòng ở cuối
        if (lines.size() > 2 && TABLE_SEPARATOR.matcher(lines.get(1).strip()).matches()) {
            header = lines.get(0) + ensureNewline(lines.get(1));
            lines = lines.subList(2, lines.size());
        }

        int budget = properties.getMaxChunkChars() - header.length();
        List<String> parts = new ArrayList<>();
        for (String rows : mergeGreedy(lines, budget)) {
            // một dòng tự nó dài hơn trần thì đứng riêng, ghi cảnh báo như mức 4
            if (rows.strip().length() > budget) {
                for (String kept : keepWhole(rows, documentId, page)) {
                    parts.add(header + kept);
                }
            } else {
                parts.add(header + rows);
            }
        }
        return parts;
    }

    // mức 4: giữ nguyên dù vượt trần, chỉ cắt khi vượt giới hạn của model
    private List<String> keepWhole(String text, String documentId, int page) {
        // gần như chắc chắn là lỗi OCR lặp (.docs/projectdevlogs/lap-chi-muc_v1.5.md, mục 2.2.6)
        log.warn("chunk vượt trần: tài liệu {} trang {}, {} ký tự", documentId, page, text.strip().length());

        int limit = properties.getModelLimitChars();
        if (text.length() <= limit) {
            return List.of(text);
        }
        // lưới an toàn cuối cùng, kho hiện tại không chạm tới
        List<String> parts = new ArrayList<>();
        for (int start = 0; start < text.length(); start += limit) {
            parts.add(text.substring(start, Math.min(text.length(), start + limit)));
        }
        return parts;
    }

    // gộp tham lam: giữ thứ tự, chỉ gộp các mảnh liền kề, tổng không vượt max
    static List<String> mergeGreedy(List<String> pieces, int max) {
        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String piece : pieces) {
            if (!current.isEmpty() && current.length() + piece.length() > max) {
                merged.add(current.toString());
                current.setLength(0);
            }
            current.append(piece);
        }
        if (!current.isEmpty()) {
            merged.add(current.toString());
        }
        return merged;
    }

    // khối là bảng khi mọi dòng có nội dung đều bắt đầu bằng |
    private static boolean isTable(String text) {
        return text.lines()
                .filter(line -> !line.isBlank())
                .allMatch(line -> line.stripLeading().startsWith("|"));
    }

    private static String ensureNewline(String line) {
        return line.endsWith("\n") ? line : line + "\n";
    }

    private static List<String> nonEmpty(String[] pieces) {
        List<String> out = new ArrayList<>();
        for (String piece : pieces) {
            if (!piece.isEmpty()) {
                out.add(piece);
            }
        }
        return out;
    }
}
