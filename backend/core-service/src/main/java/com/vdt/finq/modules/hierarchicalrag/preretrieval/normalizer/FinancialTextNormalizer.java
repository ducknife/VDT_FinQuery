package com.vdt.finq.modules.hierarchicalrag.preretrieval.normalizer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vdt.finq.common.text.VietnameseText;
import com.vdt.finq.modules.document.DocumentEnums.PhamVi;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner.NFCNormalization;

// chuẩn hoá câu hỏi và ngữ liệu về cùng một dạng (QĐ-027, logs truy-hoi_v1.3 mục 1.5).
// DÙNG CHUNG cho hai đầu: câu hỏi (normalizeQuery, lúc hỏi) và văn bản đem embed (normalizeCorpus, lúc nạp).
//
// 6 quy tắc theo thứ tự:
//   1. NFC + khoảng trắng                       (cả hai đầu, THAY vì không đổi nghĩa)
//   2. gõ không dấu -> thêm dạng có dấu          (chỉ câu hỏi)
//   3. mở rộng viết tắt theo abbreviations.yml        (cả hai đầu)
//   4. chuẩn hoá cách gọi kỳ                     (cả hai đầu)
//   5. chuẩn hoá đơn vị, số tiền về đồng         (cả hai đầu)
//   6. điền ngữ cảnh thiếu từ metadata           (chỉ câu hỏi)
// nguyên tắc: THÊM dạng chuẩn vào sau, GIỮ chữ gốc: "LNST (lợi nhuận sau thuế)".
// mọi phép tìm chạy trên bản không dấu cùng độ dài với câu gốc, rồi chèn vào câu gốc đúng vị trí
@Component
public class FinancialTextNormalizer {

    private static final Pattern SPACES = Pattern.compile("[ \\t\\u00a0]+");
    private static final Pattern YEAR = Pattern.compile("(?<!\\d)(19|20)\\d{2}(?!\\d)");
    private static final String B = "(?<![\\p{L}\\p{N}])";  // ranh giới đầu từ
    private static final String E = "(?![\\p{L}\\p{N}])";   // ranh giới cuối từ

    // quy tắc 4: các cách gọi kỳ, tìm trên bản không dấu, chữ thường
    private static final Pattern Q_SHORT = Pattern.compile(B + "q\\s*([1-4])\\s*[/.-]?\\s*((?:19|20)\\d{2})" + E);
    private static final Pattern Q_WORD = Pattern.compile(
            B + "quy\\s+(iv|iii|ii|i|[1-4])" + E + "(?:\\s*(?:[/.,-]|nam)?\\s*((?:19|20)\\d{2})" + E + ")?");
    private static final Pattern HALF = Pattern.compile(
            B + "(6t|6 thang(?! cuoi)(?: dau nam)?|ban nien|giua nien do|h1)" + E + "(?:\\s*(?:nam\\s*)?((?:19|20)\\d{2})" + E + ")?");
    private static final Pattern NINE = Pattern.compile(
            B + "(9t|9 thang(?! cuoi)(?: dau nam)?)" + E + "(?:\\s*(?:nam\\s*)?((?:19|20)\\d{2})" + E + ")?");
    private static final Pattern FISCAL = Pattern.compile(B + "(?:fy\\s*|nam tai chinh\\s+)((?:19|20)\\d{2})" + E);

    // quy tắc 5: số + đơn vị (cụm dài trước: "trăm triệu" trước "trăm"); số trơn có nhóm nghìn + "đồng/vnđ"; phần trăm
    private static final Pattern AMOUNT = Pattern.compile(
            "(?<![\\d.,])(\\d{1,3}(?:\\.\\d{3})+(?:,\\d+)?|\\d+(?:[.,]\\d+)?)\\s*(nghin ty|ngan ty|nghin ti|ngan ti|tram ty|tram ti|ty|ti|tram trieu|trieu|tr|tram nghin|tram ngan|nghin|ngan|tram|k)"
                    + "(?![\\p{L}])(?:\\s*(dong|vnd|d)(?![\\p{L}]))?");
    private static final Pattern PLAIN_DONG = Pattern.compile(
            "(?<![\\d.,])(\\d{1,3}(?:\\.\\d{3}){2,})\\s*(dong|vnd|d)(?![\\p{L}])");
    private static final Pattern PERCENT = Pattern.compile("(?<![\\d.,])(\\d+(?:[.,]\\d+)?)\\s*phan tram" + E);

    // quy tắc 6: thời gian tương đối
    private static final Pattern THIS_YEAR = Pattern.compile(B + "nam nay" + E);
    private static final Pattern LAST_YEAR = Pattern.compile(B + "nam (?:ngoai|truoc)" + E);
    private static final Pattern THIS_QUARTER = Pattern.compile(B + "quy nay" + E);
    private static final Pattern LAST_QUARTER = Pattern.compile(B + "quy truoc" + E);
    private static final Pattern LATEST = Pattern.compile(B + "(?:gan nhat|moi nhat|hien nay|hien tai)" + E);

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NFCNormalization nfc;
    private final NormalizationDictionary dictionary;
    // mỗi viết tắt một pattern, dựng sẵn một lần
    private final List<Map.Entry<NormalizationDictionary.Abbreviation, List<Pattern>>> abbreviationPatterns;

    // Spring dùng constructor này (đọc từ điển trong classpath); constructor còn lại cho test
    @Autowired
    public FinancialTextNormalizer(NFCNormalization nfc) {
        this(nfc, NormalizationDictionary.fromClasspath());
    }

    FinancialTextNormalizer(NFCNormalization nfc, NormalizationDictionary dictionary) {
        this.nfc = nfc;
        this.dictionary = dictionary;
        this.abbreviationPatterns = new ArrayList<>();
        for (NormalizationDictionary.Abbreviation a : dictionary.getAbbreviations()) {
            List<Pattern> patterns = new ArrayList<>();
            for (String form : a.getForms()) {
                // cụm 3 ký tự trở lên khớp không phân biệt hoa thường; cụm ngắn phải đúng chữ hoa
                int flags = form.replace(" ", "").length() >= 3 ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
                patterns.add(Pattern.compile(B + Pattern.quote(form) + E, flags));
            }
            abbreviationPatterns.add(Map.entry(a, patterns));
        }
    }

    // ===================== hai lối vào =====================

    // latestPeriodEnd: kỳ mới nhất có trong documents (MAX(ngay_ket_thuc_ky)), mốc của "năm nay", "quý trước"
    public NormalizedQuery normalizeQuery(String question, LocalDate latestPeriodEnd) {
        String text = cleanSpaces(nfc.toNfc(question));
        Work work = new Work(text);
        restoreDiacritics(work);
        expandAbbreviations(work);
        canonicalPeriods(work);
        canonicalAmounts(work);
        relativeTime(work, latestPeriodEnd);

        String result = work.render();
        String folded = VietnameseText.fold(result);
        String company = detectCompany(folded);
        // nói rõ "hợp nhất" thì chữ "riêng" chỉ là "chỉ riêng" ("lãi ròng hợp nhất riêng quý 3"),
        // không phải báo cáo riêng (benchmark 12/10/2026, câu D08)
        PhamVi phamVi = containsWord(folded, "hop nhat") ? PhamVi.HOP_NHAT
                : containsWord(folded, "rieng") || containsWord(folded, "cong ty me") ? PhamVi.RIENG : null;
        if (company == null) {
            work.applied.add("context:company=" + dictionary.getDefaultCompany());
        }
        if (phamVi == null) {
            work.applied.add("context:scope=hop_nhat");
        }
        return new NormalizedQuery(question, result, List.copyOf(work.amounts),
                company == null ? dictionary.getDefaultCompany() : company, company == null,
                phamVi == null ? PhamVi.HOP_NHAT : phamVi, phamVi == null, List.copyOf(work.applied));
    }

    // văn bản đem embed của một chunk (đã qua TextCleaner): chỉ quy tắc 1, 3, 4, 5.
    // không thêm dấu (ngữ liệu đã có dấu), không điền ngữ cảnh (đã có dòng tên tài liệu đầu chunk)
    public String normalizeCorpus(String text) {
        Work work = new Work(nfc.toNfc(text));
        expandAbbreviations(work);
        canonicalPeriods(work);
        canonicalAmounts(work);
        return work.render();
    }

    // ===================== các quy tắc =====================

    // quy tắc 1: gộp khoảng trắng (NFC đã chạy trước)
    private static String cleanSpaces(String text) {
        return SPACES.matcher(text).replaceAll(" ").strip();
    }

    // quy tắc 2: câu gõ KHÔNG DẤU thì dựng lại câu có dấu cho các thuật ngữ trong từ điển,
    // nối vào cuối trong ngoặc. câu đã có dấu thì không đụng (tránh "sửa" chữ người dùng gõ đúng)
    private void restoreDiacritics(Work work) {
        if (VietnameseText.hasDiacritics(work.original)) {
            return;
        }
        String folded = work.folded;
        StringBuilder restored = new StringBuilder();
        boolean changed = false;
        int i = 0;
        while (i < folded.length()) {
            String hit = null;
            if (i == 0 || !Character.isLetterOrDigit(folded.charAt(i - 1))) {
                for (String term : dictionary.getTerms()) {
                    String t = VietnameseText.fold(term);
                    int end = i + t.length();
                    if (folded.startsWith(t, i) && (end == folded.length() || !Character.isLetterOrDigit(folded.charAt(end)))) {
                        hit = term;
                        break;
                    }
                }
            }
            if (hit != null) {
                String t = VietnameseText.fold(hit);
                restored.append(hit);
                changed |= !hit.equalsIgnoreCase(work.original.substring(i, i + t.length()));
                i += t.length();
            } else {
                restored.append(work.original.charAt(i));
                i++;
            }
        }
        if (changed) {
            work.suffix = " (" + restored + ")";
            work.applied.add("diacritics");
        }
    }

    // quy tắc 3: mở rộng lần xuất hiện ĐẦU của mỗi viết tắt.
    // bỏ qua khi: đã có ngoặc giải thích ngay sau, dạng đầy đủ đã có trong câu, hoặc cụm nhập nhằng mà thiếu ngữ cảnh
    private void expandAbbreviations(Work work) {
        String foldedAll = VietnameseText.fold(work.original);
        for (Map.Entry<NormalizationDictionary.Abbreviation, List<Pattern>> entry : abbreviationPatterns) {
            NormalizationDictionary.Abbreviation a = entry.getKey();
            if (foldedAll.contains(VietnameseText.fold(a.getFullForm()))) {
                continue;
            }
            if (!a.getContext().isEmpty() && a.getContext().stream().noneMatch(c -> containsWord(foldedAll, c))) {
                continue;
            }
            for (Pattern p : entry.getValue()) {
                Matcher m = p.matcher(work.original);
                if (m.find() && !explainedAfter(work.original, m.end())) {
                    work.insert(m.end(), " (" + a.getFullForm() + ")");
                    work.applied.add("abbreviation:" + m.group());
                    break;
                }
            }
        }
    }

    // quy tắc 4: thêm cách gọi kỳ chuẩn "quý 3 năm 2024", "6 tháng đầu năm 2023", "năm 2023"
    private void canonicalPeriods(Work work) {
        String f = work.folded;
        Matcher m = Q_SHORT.matcher(f);
        while (m.find()) {
            annotate(work, m, "quý " + m.group(1) + " năm " + m.group(2), "period");
        }
        m = Q_WORD.matcher(f);
        while (m.find()) {
            String canonical = "quý " + quarterNumber(m.group(1)) + (m.group(2) == null ? "" : " năm " + m.group(2));
            annotate(work, m, canonical, "period");
        }
        m = HALF.matcher(f);
        while (m.find()) {
            annotate(work, m, "6 tháng đầu năm" + (m.group(2) == null ? "" : " " + m.group(2)), "period");
        }
        m = NINE.matcher(f);
        while (m.find()) {
            annotate(work, m, "9 tháng đầu năm" + (m.group(2) == null ? "" : " " + m.group(2)), "period");
        }
        m = FISCAL.matcher(f);
        while (m.find()) {
            annotate(work, m, "năm " + m.group(1), "period");
        }
    }

    // quy tắc 5: số tiền về đồng (ghi lại vào amounts), phần trăm về dạng "%"
    private void canonicalAmounts(Work work) {
        String f = work.folded;
        Matcher m = AMOUNT.matcher(f);
        while (m.find()) {
            long dong = VietnameseAmount.toDong(m.group(1), m.group(2));
            work.amounts.add(dong);
            annotate(work, m, VietnameseAmount.format(dong), "amount");
        }
        m = PLAIN_DONG.matcher(f);
        while (m.find()) {
            long dong = VietnameseAmount.toDong(m.group(1), "dong");
            work.amounts.add(dong);
            if (!"dong".equals(m.group(2))) {
                annotate(work, m, VietnameseAmount.format(dong), "amount");
            }
        }
        m = PERCENT.matcher(f);
        while (m.find()) {
            annotate(work, m, m.group(1) + "%", "percent");
        }
    }

    // quy tắc 6 (phần thời gian): mốc là kỳ mới nhất có số liệu, KHÔNG phải ngày hệ thống
    private void relativeTime(Work work, LocalDate latest) {
        if (latest == null) {
            return;
        }
        String f = work.folded;
        int year = latest.getYear();
        int quarter = (latest.getMonthValue() - 1) / 3 + 1;
        boolean explicitYear = YEAR.matcher(f).find();

        Matcher m = THIS_YEAR.matcher(f);
        while (m.find()) {
            annotate(work, m, "năm " + year, "relative_time");
        }
        if (!explicitYear) {
            m = LAST_YEAR.matcher(f);
            while (m.find()) {
                annotate(work, m, "năm " + (year - 1), "relative_time");
            }
        }
        m = THIS_QUARTER.matcher(f);
        while (m.find()) {
            annotate(work, m, "quý " + quarter + " năm " + year, "relative_time");
        }
        m = LAST_QUARTER.matcher(f);
        while (m.find()) {
            int q = quarter == 1 ? 4 : quarter - 1;
            annotate(work, m, "quý " + q + " năm " + (quarter == 1 ? year - 1 : year), "relative_time");
        }
        m = LATEST.matcher(f);
        while (m.find()) {
            annotate(work, m, "kỳ " + latest.format(DMY), "relative_time");
        }
    }

    // ===================== tiện ích =====================

    // thêm dạng chuẩn sau đoạn khớp, trừ khi đoạn gốc đã đúng dạng chuẩn hoặc đã có ngoặc giải thích
    private static void annotate(Work work, Matcher m, String canonical, String rule) {
        String matched = m.group();
        if (VietnameseText.fold(matched).equals(VietnameseText.fold(canonical)) || explainedAfter(work.original, m.end())) {
            return;
        }
        work.insert(m.end(), " (" + canonical + ")");
        work.applied.add(rule + ":" + work.original.substring(m.start(), m.end()));
    }

    private static boolean explainedAfter(String text, int end) {
        int i = end;
        while (i < text.length() && text.charAt(i) == ' ') {
            i++;
        }
        return i < text.length() && text.charAt(i) == '(';
    }

    private static boolean containsWord(String folded, String word) {
        return Pattern.compile(B + Pattern.quote(word) + E).matcher(folded).find();
    }

    private String detectCompany(String folded) {
        for (Map.Entry<String, List<String>> e : dictionary.getCompanies().entrySet()) {
            for (String alias : e.getValue()) {
                if (containsWord(folded, alias)) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    private static int quarterNumber(String token) {
        return switch (token) {
            case "i", "1" -> 1;
            case "ii", "2" -> 2;
            case "iii", "3" -> 3;
            default -> 4;
        };
    }

    // trạng thái của một lượt chuẩn hoá: câu gốc, bản không dấu cùng độ dài, các chỗ cần chèn
    private static final class Work {
        private final String original;
        private final String folded;
        // vị trí -> chuỗi chèn; nhiều chuỗi cùng vị trí thì nối theo thứ tự thêm vào
        private final TreeMap<Integer, StringBuilder> inserts = new TreeMap<>();
        private final List<Long> amounts = new ArrayList<>();
        private final List<String> applied = new ArrayList<>();
        private String suffix = "";

        private Work(String original) {
            this.original = original;
            this.folded = VietnameseText.foldKeepLength(original);
        }

        private void insert(int position, String text) {
            inserts.computeIfAbsent(position, k -> new StringBuilder()).append(text);
        }

        private String render() {
            StringBuilder out = new StringBuilder();
            int last = 0;
            for (Map.Entry<Integer, StringBuilder> e : inserts.entrySet()) {
                out.append(original, last, e.getKey()).append(e.getValue());
                last = e.getKey();
            }
            return out.append(original.substring(last)).append(suffix).toString();
        }
    }
}
