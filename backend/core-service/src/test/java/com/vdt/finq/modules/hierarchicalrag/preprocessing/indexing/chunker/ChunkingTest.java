package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.vdt.finq.common.properties.IndexingProperties;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner.NFCNormalization;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner.TextCleaner;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.stitcher.TableStitcher;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.stitcher.TextStitcher;

// test thuật toán chunk, không cần khởi động Spring
class ChunkingTest {

    private static final Path CORPUS = Path.of("../../data/ocr/CTR");

    private final IndexingProperties properties = new IndexingProperties(4000, 500, 50000);
    private final TextStitcher textStitcher = new TextStitcher(properties);
    private final TableStitcher tableStitcher = new TableStitcher();
    private final Chunking chunking = new Chunking(properties, tableStitcher, textStitcher);
    private final TextCleaner cleaner = new TextCleaner(new NFCNormalization());

    // ví dụ chạy tay ở mục 2.7.3 của log, với MAX tạm là 1.500
    @Test
    void mergeGreedyMatchesHandRun() {
        List<String> pieces = Stream.of(148, 496, 814, 512, 501, 456, 449).map("x"::repeat).toList();
        List<Integer> lengths = Chunking.mergeGreedy(pieces, 1500).stream().map(String::length).toList();
        assertThat(lengths).containsExactly(1458, 1469, 449);
    }

    // câu "Vốn chủ sở hữu tăng | từ 867 tỷ" bị cắt qua trang 2 -> 3 của BC-BDH 2021
    @Test
    void detectsSentenceCutAcrossPages() {
        String page2 = "### 3.1. Những mặt đạt được\n\n"
                + "- Các chỉ số tài chính đạt kế hoạch (Dòng tiền dương 163,5 tỷ); Vốn chủ sở hữu tăng";
        String page3 = "từ 867 tỷ lên 1.061 tỷ; Hệ số nợ/tổng tài sản 0,73 lần).\n\n"
                + "- Lĩnh vực Vận hành khai thác: tiếp tục duy trì tốt.";

        assertThat(textStitcher.isCut(page2, page3)).isTrue();
        // không dừng ở dấu chấm của 1.061
        assertThat(textStitcher.head(page3)).isEqualTo("từ 867 tỷ lên 1.061 tỷ; Hệ số nợ/tổng tài sản 0,73 lần).");
        assertThat(textStitcher.tail(page2)).endsWith("Vốn chủ sở hữu tăng");

        List<String> pages = List.of(page2, page3);
        assertThat(textStitcher.stitch(pages, 0)).contains("Vốn chủ sở hữu tăng (tiếp trang 2) từ 867 tỷ lên 1.061 tỷ");
        assertThat(textStitcher.stitch(pages, 1)).startsWith("(từ trang 1) - Các chỉ số tài chính");
    }

    // các ranh giới không bị cắt
    @Test
    void ignoresBoundariesThatAreNotCuts() {
        // câu đã kết thúc
        assertThat(textStitcher.isCut("Doanh thu tăng mạnh.", "từ năm 2019 trở đi")).isFalse();
        // trang sau là gạch đầu dòng mới
        assertThat(textStitcher.isCut("Doanh thu đạt 3.369 tỷ", "- Lĩnh vực Xây dựng:")).isFalse();
        // trang trước kết thúc bằng ghi chú in nghiêng
        assertThat(textStitcher.isCut("*Xem thông tin chi tiết tại Thuyết minh số 35*", "kỳ kế toán từ ngày 01/01/2019")).isFalse();
        // trang trước kết thúc bằng bảng
        assertThat(textStitcher.isCut("| 1 | Doanh thu | 100 |", "tiếp theo là")).isFalse();
        // trang sau bắt đầu bằng chữ hoa: bỏ sót, chấp nhận được
        assertThat(textStitcher.isCut("ký hợp đồng với", "Viettel Global")).isFalse();
        // số trang ở cuối trang trước được bỏ qua
        assertThat(textStitcher.isCut("Vốn chủ sở hữu tăng\n\n12", "từ 867 tỷ")).isTrue();
    }

    // bảng chạy sang trang sau, OCR coi dòng số liệu là tiêu đề (BCTC HN quý 4/2023, trang 7 -> 8)
    @Test
    void repairsTableHeaderAcrossPages() {
        String header = "| Mã số | CHỈ TIÊU | Thuyết minh | Quý IV/2023 VND | Năm 2023 VND |";
        String page7 = "Kết quả kinh doanh\n\n" + header + "\n|---|---|---|---|---|\n"
                + "| 50 | 14. Tổng lợi nhuận trước thuế | | 180.000.000.000 | 640.000.000.000 |";
        String page8 = "| 51 | 15. Chi phí thuế TNDN hiện hành | 30 | 36.240.869.667 | 129.639.757.405 |\n"
                + "|---|---|---|---|---|\n"
                + "| 60 | 17. Lợi nhuận sau thuế | | 143.759.130.333 | 510.360.242.595 |";

        String repaired = tableStitcher.repairTableHeaders(List.of(page7, page8)).get(1);

        assertThat(repaired).startsWith("(tiêu đề bảng từ trang 1)\n\n" + header + "\n|---|---|---|---|---|\n| 51 |");
        // dòng |---| giả sau dòng số liệu đã bị bỏ
        assertThat(repaired.split("\n")).filteredOn(l -> l.startsWith("|---")).hasSize(1);
    }

    // bảng trải 3 trang: trang 3 phải nhận tiêu đề thật của trang 1,
    // không được nhận dòng số liệu mà OCR coi là tiêu đề ở trang 2
    @Test
    void repairPropagatesAcrossThreePages() {
        String header = "| Mã số | Chỉ tiêu | Năm nay | Năm trước |";
        String page1 = header + "\n|---|---|---|---|\n| 10 | Doanh thu | 1.000 | 900 |";
        String page2 = "| 20 | Giá vốn | 800 | 700 |\n|---|---|---|---|\n| 30 | Lãi gộp | 200 | 200 |";
        String page3 = "| 40 | Chi phí bán hàng | 50 | 40 |\n|---|---|---|---|\n| 50 | Lợi nhuận | 150 | 160 |";

        List<String> repaired = tableStitcher.repairTableHeaders(List.of(page1, page2, page3));

        assertThat(repaired.get(1)).contains("(tiêu đề bảng từ trang 1)\n\n" + header);
        assertThat(repaired.get(2)).contains("(tiêu đề bảng từ trang 2)\n\n" + header);
        assertThat(repaired.get(2)).doesNotContain("| 20 | Giá vốn | 800 | 700 |\n|---|");
    }

    // tiêu đề hai tầng (BCTC HN 2024, trang 17 -> 18): chép cả dòng "Giá trị | Dự phòng"
    @Test
    void repairsTwoLevelTableHeader() {
        String page17 = "| | 31/12/2024 | | 01/01/2024 | |\n|---|---|---|---|---|\n"
                + "| | Giá trị | Dự phòng | Giá trị | Dự phòng |\n"
                + "| Phải thu khách hàng | | | | |\n"
                + "| Bên liên quan | 500.000.000 | (10.000.000) | 400.000.000 | (9.000.000) |";
        String page18 = "| Bên khác | 1.177.755.246.484 | (88.856.176.675) | 1.076.091.866.772 | (102.504.855.107) |\n"
                + "| :--- | :--- | :--- | :--- | :--- |\n"
                + "| - Công ty FLC Faros | 20.664.108.829 | (20.664.108.829) | 46.399.570.095 | (46.399.570.095) |";

        String repaired = tableStitcher.repairTableHeader(page17, page18, 17);

        assertThat(repaired).startsWith("(tiêu đề bảng từ trang 17)\n\n"
                + "| | 31/12/2024 | | 01/01/2024 | |\n|---|---|---|---|---|\n"
                + "| | Giá trị | Dự phòng | Giá trị | Dự phòng |\n"
                + "| Bên khác |");
        // dòng nhóm "Phải thu khách hàng" chỉ có một ô chữ, không bị coi là tiêu đề phụ
        assertThat(repaired).doesNotContain("Phải thu khách hàng");
    }

    // các bảng không được sửa: OCR đã lặp lại tiêu đề thật, khác số cột, trang trước không có tiêu đề
    @Test
    void leavesOtherTablesAlone() {
        String page7 = "| | 6 tháng 2024 | 6 tháng 2023 |\n|---|---|---|\n| Doanh thu | 100 | 90 |";
        String repeated = "| | 6 tháng 2024 | 6 tháng 2023 |\n|---|---|---|\n| Giá vốn | 80 | 70 |";
        String otherShape = "| Tổng Công ty Cổ phần Công trình Viettel | Báo cáo tài chính hợp nhất |\n|---|---|";
        String noHeaderBefore = "| Doanh thu | 100 | 90 |";

        assertThat(tableStitcher.repairTableHeader(page7, repeated, 1)).isEqualTo(repeated);
        assertThat(tableStitcher.repairTableHeader(page7, otherShape, 1)).isEqualTo(otherShape);
        assertThat(tableStitcher.repairTableHeader(noHeaderBefore, repeated, 1)).isEqualTo(repeated);
    }

    // bảng dài bị cắt: phần nào cũng bắt đầu bằng tiêu đề cột
    @Test
    void splitTableRepeatsHeader() {
        StringBuilder table = new StringBuilder("| Chỉ tiêu | Năm nay | Năm trước |\n|---|---|---|\n");
        for (int i = 0; i < 120; i++) {
            table.append("| Khoản mục số ").append(i).append(" | 1.234.567.890 | 987.654.321 |\n");
        }
        List<Chunk> chunks = chunking.chunk("TEST", List.of(table.toString()));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> {
            assertThat(c.getText()).startsWith("| Chỉ tiêu | Năm nay | Năm trước |\n|---|---|---|");
            assertThat(c.getText().length()).isLessThanOrEqualTo(4000);
        });
    }

    // trang ngắn giữ nguyên thành một chunk
    @Test
    void shortPageStaysWhole() {
        List<Chunk> chunks = chunking.chunk("TEST", List.of("# Tiêu đề\n\nNội dung ngắn."));
        assertThat(chunks).singleElement().satisfies(c -> {
            assertThat(c.getPage()).isEqualTo(1);
            assertThat(c.getPart()).isZero();
        });
    }

    // ví dụ chạy tay ở mục 2.7.1 của log: trang 6.969 ký tự, 14 mục -> 2 chunk
    @Test
    void annualReportPage40SplitsIntoTwoChunks() throws IOException {
        Path file = CORPUS.resolve("02_bao_cao_thuong_nien/2020/thuong_nien/BaoCaoThuongNien_2020_Final-5/trang_040.md");
        assumeTrue(Files.exists(file), "không có dữ liệu OCR trên máy này");

        String page = cleaner.clean(Files.readString(file));
        List<Chunk> chunks = chunking.chunk("BCTN-2020", List.of(page));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(1).getText()).startsWith("# 35.");
        chunks.forEach(c -> System.out.printf("trang_040 phần %d: %d ký tự%n", c.getPart(), c.getText().length()));
    }

    // chạy cả kho để so với số đo trong log
    @Test
    void wholeCorpusStatistics() throws IOException {
        assumeTrue(Files.isDirectory(CORPUS), "không có dữ liệu OCR trên máy này");

        Map<Path, Map<Integer, Path>> documents = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(CORPUS)) {
            walk.filter(p -> p.getFileName().toString().matches("trang_\\d+\\.md"))
                .forEach(p -> documents.computeIfAbsent(p.getParent(), k -> new TreeMap<>())
                        .put(Integer.parseInt(p.getFileName().toString().replaceAll("\\D", "")), p));
        }

        int pageCount = 0;
        int stitched = 0;
        int tablesRepaired = 0;
        List<Chunk> all = new ArrayList<>();
        for (Map.Entry<Path, Map<Integer, Path>> doc : documents.entrySet()) {
            // giữ đúng số trang: trang thiếu được thay bằng chuỗi rỗng
            int last = ((TreeMap<Integer, Path>) doc.getValue()).lastKey();
            List<String> pages = new ArrayList<>();
            for (int n = 1; n <= last; n++) {
                Path p = doc.getValue().get(n);
                pages.add(p == null ? "" : cleaner.clean(Files.readString(p)));
            }
            for (int i = 0; i + 1 < pages.size(); i++) {
                if (!pages.get(i).isBlank() && !pages.get(i + 1).isBlank()
                        && textStitcher.isCut(pages.get(i), pages.get(i + 1))) {
                    stitched++;
                }
            }
            // đếm trang được sửa tiêu đề bảng, theo đúng cách Chunking gọi (cả tài liệu một lượt)
            List<String> repaired = tableStitcher.repairTableHeaders(pages);
            for (int i = 1; i < pages.size(); i++) {
                if (!repaired.get(i).equals(pages.get(i))) {
                    tablesRepaired++;
                    if (tablesRepaired <= 3) {
                        System.out.printf("   sửa tiêu đề bảng: %s trang %d%n", CORPUS.relativize(doc.getKey()), i + 1);
                    }
                }
            }
            pageCount += doc.getValue().size();
            all.addAll(chunking.chunk(CORPUS.relativize(doc.getKey()).toString(), pages));
        }

        List<Integer> lengths = all.stream().map(c -> c.getText().length()).sorted().toList();
        long oversized = lengths.stream().filter(l -> l > 4000).count();
        long splitPages = all.stream().filter(c -> c.getPart() == 1).count();

        System.out.printf("tài liệu %d, trang %d, chunk %d%n", documents.size(), pageCount, all.size());
        System.out.printf("ranh giới được gối câu %d, bảng được sửa tiêu đề %d, trang bị cắt %d, chunk vượt trần %d%n",
                stitched, tablesRepaired, splitPages, oversized);
        System.out.printf("độ dài chunk: trung vị %d, p90 %d, lớn nhất %d%n",
                lengths.get(lengths.size() / 2), lengths.get(lengths.size() * 9 / 10), lengths.get(lengths.size() - 1));
        all.stream().filter(c -> c.getText().length() > 4000)
                .sorted(Comparator.comparingInt((Chunk c) -> c.getText().length()).reversed()).limit(5)
                .forEach(c -> System.out.printf("   vượt trần: %s trang %d phần %d, %d ký tự%n",
                        c.getDocumentId(), c.getPage(), c.getPart(), c.getText().length()));

        // mọi trang có nội dung đều phải sinh ra ít nhất một chunk
        assertThat(all.stream().map(c -> c.getDocumentId() + "#" + c.getPage()).distinct().count()).isEqualTo(pageCount);
    }
}
