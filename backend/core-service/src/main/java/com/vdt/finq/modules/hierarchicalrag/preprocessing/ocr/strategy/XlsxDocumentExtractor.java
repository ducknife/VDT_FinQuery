package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.strategy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.DocumentOcrResult;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.PageStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// bảng tính vốn đã là bảng, mỗi sheet thành một đơn vị
@Slf4j
@Component
@RequiredArgsConstructor
public class XlsxDocumentExtractor implements DocumentExtractor {

    private final PageStore pageStore;

    @Override
    public boolean supports(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xlsm");
    }

    @Override
    public DocumentOcrResult extract(Path file, String relativePath) throws IOException {
        int written = 0;
        try (InputStream in = Files.newInputStream(file);
             Workbook workbook = new XSSFWorkbook(in)) {

            // DataFormatter trả về đúng chữ người dùng thấy trong excel,
            // nên 1.234.567 vẫn là 1.234.567 thay vì thành 1234567.0
            DataFormatter formatter = new DataFormatter();

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String markdown = renderSheet(sheet, formatter);
                if (markdown.isBlank()) {
                    continue;
                }
                written++;
                pageStore.write(relativePath, written, markdown);
            }
        }
        log.info("{}: {} sheets written", file.getFileName(), written);
        return new DocumentOcrResult(written, 0, written, 0);
    }

    private String renderSheet(Sheet sheet, DataFormatter formatter) {
        int lastRow = sheet.getLastRowNum();
        if (lastRow < 0) {
            return "";
        }
        // dòng rộng nhất quyết định số cột, dòng ngắn hơn được bù ô trống
        int columns = 0;
        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                columns = Math.max(columns, row.getLastCellNum());
            }
        }
        if (columns <= 0) {
            return "";
        }

        StringBuilder out = new StringBuilder("## ").append(sheet.getSheetName()).append("\n\n");
        boolean headerWritten = false;
        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            StringBuilder line = new StringBuilder("| ");
            boolean empty = true;
            for (int c = 0; c < columns; c++) {
                String text = "";
                if (row != null) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        text = formatter.formatCellValue(cell).strip().replace("\n", " ");
                        // dấu | trong ô sẽ làm vỡ dòng bảng markdown
                        text = text.replace("|", "\\|");
                        empty = empty && text.isEmpty();
                    }
                }
                line.append(text).append(" | ");
            }
            if (empty) {
                continue;
            }
            out.append(line).append('\n');
            if (!headerWritten) {
                out.append("|").append(" --- |".repeat(columns)).append('\n');
                headerWritten = true;
            }
        }
        return out.toString();
    }
}
