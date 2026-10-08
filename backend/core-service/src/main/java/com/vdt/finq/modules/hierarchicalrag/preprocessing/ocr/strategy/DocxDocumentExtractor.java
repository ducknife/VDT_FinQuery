package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.strategy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.DocumentOcrResult;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.PageStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// word đã lưu đoạn văn và ô bảng dưới dạng có cấu trúc, nên không cần nhận dạng gì.
// đưa qua OCR thì sẽ mất cấu trúc đó
@Slf4j
@Component
@RequiredArgsConstructor
public class DocxDocumentExtractor implements DocumentExtractor {

    // docx không có trang cho tới khi được dàn trang, nên ta cắt theo heading
    // và mở đơn vị mới khi đơn vị hiện tại dài quá mức này
    private static final int MAX_UNIT_CHARS = 4_000;

    private final PageStore pageStore;

    @Override
    public boolean supports(String fileName) {
        return fileName.toLowerCase().endsWith(".docx");
    }

    @Override
    public DocumentOcrResult extract(Path file, String relativePath) throws IOException {
        List<String> units = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        try (InputStream in = Files.newInputStream(file);
             XWPFDocument document = new XWPFDocument(in)) {

            // duyệt các phần tử theo thứ tự để đoạn văn và bảng giữ đúng vị trí
            for (IBodyElement element : document.getBodyElements()) {
                String block = switch (element) {
                    case XWPFParagraph paragraph -> renderParagraph(paragraph);
                    case XWPFTable table -> renderTable(table);
                    default -> "";
                };
                if (block.isBlank()) {
                    continue;
                }
                // gặp heading thì mở đơn vị mới, đơn vị quá dài cũng vậy
                boolean isHeading = block.startsWith("#");
                if (!current.isEmpty() && (isHeading || current.length() > MAX_UNIT_CHARS)) {
                    units.add(current.toString().strip());
                    current.setLength(0);
                }
                current.append(block).append("\n\n");
            }
        }
        if (!current.isEmpty()) {
            units.add(current.toString().strip());
        }

        int written = 0;
        for (int i = 0; i < units.size(); i++) {
            pageStore.write(relativePath, i + 1, units.get(i));
            written++;
        }
        log.info("{}: {} units written", file.getFileName(), written);
        return new DocumentOcrResult(written, 0, written, 0);
    }

    // style heading của word thành heading markdown để chunker cắt được theo đó
    private String renderParagraph(XWPFParagraph paragraph) {
        String text = paragraph.getText().strip();
        if (text.isEmpty()) {
            return "";
        }
        String style = paragraph.getStyle();
        if (style != null && style.toLowerCase().startsWith("heading")) {
            int level = style.replaceAll("\\D", "").isEmpty()
                    ? 2
                    : Integer.parseInt(style.replaceAll("\\D", ""));
            return "#".repeat(Math.min(level, 6)) + " " + text;
        }
        return text;
    }

    // dòng đầu làm tiêu đề, đúng như bảng markdown yêu cầu
    private String renderTable(XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int r = 0; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            out.append("| ");
            for (XWPFTableCell cell : cells) {
                // xuống dòng trong ô sẽ làm vỡ dòng bảng markdown
                out.append(cell.getText().strip().replace("\n", " ")).append(" | ");
            }
            out.append('\n');
            if (r == 0) {
                out.append("|").append(" --- |".repeat(cells.size())).append('\n');
            }
        }
        return out.toString();
    }
}
