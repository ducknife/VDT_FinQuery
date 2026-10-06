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

// word already stores paragraphs and table cells as structure, so there is nothing
// to recognise. sending it through ocr would throw that structure away
@Slf4j
@Component
@RequiredArgsConstructor
public class DocxDocumentExtractor implements DocumentExtractor {

    // a docx has no pages until it is laid out, so we cut on headings instead and
    // start a new unit once one grows past this
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

            // walk body elements in order so paragraphs and tables keep their position
            for (IBodyElement element : document.getBodyElements()) {
                String block = switch (element) {
                    case XWPFParagraph paragraph -> renderParagraph(paragraph);
                    case XWPFTable table -> renderTable(table);
                    default -> "";
                };
                if (block.isBlank()) {
                    continue;
                }
                // a heading starts a new unit, and so does an overlong one
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

    // word heading styles become markdown headings so the chunker can cut on them
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

    // first row becomes the header, which is what markdown tables need
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
                // a newline inside a cell would break the markdown row
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
