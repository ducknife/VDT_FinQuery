package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.stitcher;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.vdt.finq.common.properties.IndexingProperties;

import lombok.RequiredArgsConstructor;

// gối văn bản có chọn lọc: chỉ ở ranh giới trang nào câu bị cắt ngang
// thì chép mẩu câu đó sang trang kề bên, để chunk nào cũng có trọn câu
//
// một ranh giới bị coi là cắt ngang khi thoả CẢ HAI điều kiện:
//   A. dòng cuối trang trước còn "mở": không phải bảng, heading, dòng kẻ,
//      ghi chú in nghiêng/đậm, và không kết thúc bằng dấu câu
//   B. dòng đầu trang sau "nói tiếp": không phải bảng, heading, gạch đầu dòng,
//      và bắt đầu bằng chữ thường
// đo trên kho: 194 chỗ, lấy mẫu 8 chỗ đều đúng
@Component
@RequiredArgsConstructor
public class TextStitcher {

    // dòng chỉ có số trang: "12", "- 12 -"
    private static final Pattern PAGE_NUMBER = Pattern.compile("\\s*-?\\s*\\d{1,3}\\s*-?\\s*");

    // dòng đã đóng câu
    private static final Pattern CLOSED = Pattern.compile("[.:;!?)”\"]$");

    // dòng kẻ ngang, không bao giờ là một phần của câu
    private static final Pattern RULE = Pattern.compile("^(?:-{3,}|\\*{3,}|_{3,})$");

    // cả dòng in nghiêng hoặc in đậm, thường là ghi chú
    private static final Pattern EMPHASIS = Pattern.compile("^(\\*{1,2}|_).+\\1$");

    // hết câu: . ! ? đứng trước khoảng trắng hoặc cuối dòng, nên 1.061 không bị tính
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?](?=\\s|$)");

    private final IndexingProperties properties;

    // trả về trang thứ index (đếm từ 0) đã gối thêm mẩu câu của hai trang kề bên nếu cần
    public String stitch(List<String> pages, int index) {
        String page = pages.get(index);
        StringBuilder text = new StringBuilder();

        // đầu câu nằm ở trang trước: chép đuôi trang trước lên đầu
        if (index > 0 && isCut(pages.get(index - 1), page)) {
            text.append("(từ trang ").append(index).append(") ")
                .append(tail(pages.get(index - 1))).append(' ');
        }
        text.append(page);

        // cuối câu nằm ở trang sau: chép đầu trang sau xuống cuối
        if (index + 1 < pages.size() && isCut(page, pages.get(index + 1))) {
            text.append(" (tiếp trang ").append(index + 2).append(") ")
                .append(head(pages.get(index + 1)));
        }
        return text.toString();
    }

    // ranh giới giữa hai trang có cắt ngang câu không
    public boolean isCut(String page, String nextPage) {
        String last = lastLine(page);
        String first = firstLine(nextPage);
        if (last == null || first == null) {
            return false;
        }
        return isOpen(last) && isContinuation(first);
    }

    // đuôi trang: phần dòng cuối tính từ dấu hết câu cuối cùng
    public String tail(String page) {
        String line = lastLine(page);
        int start = 0;
        Matcher m = SENTENCE_END.matcher(line);
        while (m.find()) {
            start = m.end();
        }
        String fragment = line.substring(start).strip();
        if (fragment.isEmpty()) {
            fragment = line;
        }
        // quá dài thì chỉ giữ phần cuối (sát ranh giới), cắt ở khoảng trắng
        int max = properties.getStitchMaxChars();
        if (fragment.length() > max) {
            String cut = fragment.substring(fragment.length() - max);
            int space = cut.indexOf(' ');
            fragment = space > 0 ? cut.substring(space + 1) : cut;
        }
        return fragment;
    }

    // đầu trang: phần dòng đầu tính tới dấu hết câu đầu tiên
    public String head(String page) {
        String line = firstLine(page);
        Matcher m = SENTENCE_END.matcher(line);
        String fragment = m.find() ? line.substring(0, m.end()) : line;
        // quá dài thì chỉ giữ phần đầu (sát ranh giới), cắt ở khoảng trắng
        int max = properties.getStitchMaxChars();
        if (fragment.length() > max) {
            String cut = fragment.substring(0, max);
            int space = cut.lastIndexOf(' ');
            fragment = space > 0 ? cut.substring(0, space) : cut;
        }
        return fragment;
    }

    // điều kiện A: câu ở cuối trang chưa kết thúc
    private static boolean isOpen(String line) {
        if (line.startsWith("|") || line.startsWith("#")) {
            return false;
        }
        if (RULE.matcher(line).matches() || EMPHASIS.matcher(line).matches()) {
            return false;
        }
        return !CLOSED.matcher(line).find();
    }

    // điều kiện B: đầu trang sau đang nói tiếp câu trước
    private static boolean isContinuation(String line) {
        if (line.startsWith("|") || line.startsWith("#")
                || line.startsWith("-") || line.startsWith("*") || line.startsWith("+")) {
            return false;
        }
        // nhận đúng chữ thường tiếng Việt như đ, ư, ơ
        return Character.isLowerCase(line.codePointAt(0));
    }

    // dòng cuối có nội dung, bỏ qua dòng trống và dòng số trang
    private static String lastLine(String page) {
        List<String> lines = page.lines().toList();
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i).strip();
            if (!line.isEmpty() && !PAGE_NUMBER.matcher(line).matches()) {
                return line;
            }
        }
        return null;
    }

    // dòng đầu có nội dung, bỏ qua dòng trống và dòng số trang
    private static String firstLine(String page) {
        for (String raw : page.lines().toList()) {
            String line = raw.strip();
            if (!line.isEmpty() && !PAGE_NUMBER.matcher(line).matches()) {
                return line;
            }
        }
        return null;
    }
}
