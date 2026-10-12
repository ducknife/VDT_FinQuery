package com.vdt.finq.modules.hierarchicalrag.preretrieval.filter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.vdt.finq.common.text.VietnameseText;
import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.hierarchicalrag.preretrieval.normalizer.NormalizedQuery;

// rút điều kiện lọc từ câu hỏi đã chuẩn hoá (QĐ-030); chỉ lọc khi câu hỏi nói rõ
@Component
public class FilterExtractor {

    private static final int MIN_YEAR = 2010;
    private static final int MAX_YEAR = 2039;

    // năm đứng riêng, không dính vào số tiền ("1.234.567") hay số dài hơn
    private static final Pattern YEAR = Pattern.compile("(?<![\\d.,])(20[1-3]\\d)(?!\\d)");
    // "quy 3", "q3/2024"; số La Mã đã được bộ chuẩn hoá thêm dạng số trong ngoặc
    private static final Pattern QUARTER = Pattern.compile("(?<![\\p{L}\\d])(?:quy\\s*|q)([1-4])(?!\\d)");
    // ngày cuối kỳ "30/06/2026" -> kỳ theo tháng
    private static final Pattern PERIOD_END = Pattern.compile("(?<!\\d)(\\d{1,2})/(\\d{1,2})/(20\\d{2})(?!\\d)");
    private static final Pattern HALF_YEAR = Pattern.compile(
            "ban nien|6 thang dau nam|sau thang dau nam|nua dau nam");
    private static final Pattern FULL_YEAR = Pattern.compile(
            "ca nam|cuoi nam|nam tai chinh|da kiem toan|bao cao nam");

    // loại tài liệu chỉ lọc khi người hỏi gọi đúng tên (dạng không dấu, chữ thường)
    private static final Map<Pattern, List<LoaiTaiLieu>> DOCUMENT_TYPES = new LinkedHashMap<>();

    static {
        // không lọc theo chữ "BCTC": người hỏi dùng nó để nói phạm vi số, đáp án có thể ở công văn, BCTN
        type(List.of(LoaiTaiLieu.BCTN), "bao cao thuong nien", "bctn");
        type(List.of(LoaiTaiLieu.NGHI_QUYET), "nghi quyet", "bien ban hop");
        type(List.of(LoaiTaiLieu.TO_TRINH), "to trinh");
        type(List.of(LoaiTaiLieu.BAO_CAO_PT), "phat trien ben vung");
        type(List.of(LoaiTaiLieu.GIAI_TRINH), "giai trinh", "cong van");
        type(List.of(LoaiTaiLieu.DINH_CHINH), "dinh chinh");
    }

    // hỏi kế hoạch, định hướng năm Y: nội dung nằm trong báo cáo năm Y-1 (BCTN 2024 viết định hướng 2025)
    private static final Pattern PLAN = Pattern.compile("ke hoach|dinh huong|muc tieu|khau hieu");
    // hỏi số trình bày lại của năm Y: số đó nằm ở cột so sánh của báo cáo năm Y+1
    private static final Pattern RESTATED = Pattern.compile("trinh bay lai|dieu chinh hoi to|so lieu so sanh");
    // "từ 2022 đến nay": kéo tới năm mới nhất trong kho
    private static final Pattern UNTIL_NOW = Pattern.compile("den nay|toi nay|hien nay|hien tai");

    public QueryFilter extract(NormalizedQuery query) {
        return extract(query, null);
    }

    // latestYear: năm của kỳ mới nhất trong kho (cho "đến nay"); null thì bỏ qua
    public QueryFilter extract(NormalizedQuery query, Integer latestYear) {
        String folded = VietnameseText.fold(query.getText());
        List<String> applied = new ArrayList<>();

        Set<Integer> nams = new TreeSet<>();
        Matcher year = YEAR.matcher(folded);
        while (year.find()) {
            int y = Integer.parseInt(year.group(1));
            if (y >= MIN_YEAR && y <= MAX_YEAR) {
                nams.add(y);
            }
        }
        if (!nams.isEmpty() && latestYear != null && UNTIL_NOW.matcher(folded).find()) {
            for (int y = ((TreeSet<Integer>) nams).first(); y <= latestYear; y++) {
                nams.add(y);
            }
        }

        // năm trên tài liệu là năm ban hành (QĐ-032): văn bản sự kiện năm Y nằm ở Y..Y+2;
        // báo cáo định kỳ đúng năm Y, thêm Y-1 khi hỏi kế hoạch, Y+1 khi hỏi số trình bày lại
        Set<Integer> periodNams = new TreeSet<>(nams);
        Set<Integer> eventNams = new TreeSet<>();
        boolean plan = PLAN.matcher(folded).find();
        boolean restated = RESTATED.matcher(folded).find();
        for (int y : nams) {
            eventNams.add(y);
            eventNams.add(y + 1);
            eventNams.add(y + 2);
            if (plan) {
                periodNams.add(y - 1);
            }
            if (restated) {
                periodNams.add(y + 1);
            }
        }

        Set<Ky> kys = EnumSet.noneOf(Ky.class);
        Matcher quarter = QUARTER.matcher(folded);
        while (quarter.find()) {
            kys.add(Ky.valueOf("Q" + quarter.group(1)));
        }
        Matcher end = PERIOD_END.matcher(folded);
        while (end.find()) {
            kys.addAll(kysEndingInMonth(Integer.parseInt(end.group(2))));
        }
        if (HALF_YEAR.matcher(folded).find()) {
            // số 6 tháng đầu năm có ở báo cáo bán niên và cột luỹ kế của báo cáo quý 2
            kys.add(Ky.BAN_NIEN);
            kys.add(Ky.Q2);
        }
        if (FULL_YEAR.matcher(folded).find()) {
            // số cả năm, số cuối năm: báo cáo năm và báo cáo quý 4
            kys.add(Ky.NAM);
            kys.add(Ky.Q4);
        }

        Set<LoaiTaiLieu> loais = EnumSet.noneOf(LoaiTaiLieu.class);
        DOCUMENT_TYPES.forEach((pattern, types) -> {
            if (pattern.matcher(folded).find()) {
                loais.addAll(types);
            }
        });

        applied.add("ma_cong_ty:" + query.getMaCongTy() + (query.isMaCongTyMacDinh() ? " (mặc định)" : ""));
        nams.forEach(n -> applied.add("nam:" + n));
        if (!nams.isEmpty()) {
            applied.add("nam_bao_cao:" + periodNams + (plan ? " (hỏi kế hoạch: +năm trước)" : "")
                    + (restated ? " (số trình bày lại: +năm sau)" : ""));
            applied.add("nam_van_ban_su_kien:" + eventNams);
        }
        kys.forEach(k -> applied.add("ky:" + k.getValue()));
        applied.add("pham_vi:" + query.getPhamVi().getValue() + (query.isPhamViMacDinh() ? " (mặc định)" : ""));
        loais.forEach(l -> applied.add("loai:" + l.getValue()));

        return new QueryFilter(query.getMaCongTy(), List.copyOf(nams), List.copyOf(periodNams),
                List.copyOf(eventNams), List.copyOf(kys), query.getPhamVi(), List.copyOf(loais), List.copyOf(applied));
    }

    // 31/03 -> Q1; 30/06 -> Q2 + bán niên; 30/09 -> Q3; 31/12 -> Q4 + năm
    private static List<Ky> kysEndingInMonth(int month) {
        return switch (month) {
            case 3 -> List.of(Ky.Q1);
            case 6 -> List.of(Ky.Q2, Ky.BAN_NIEN);
            case 9 -> List.of(Ky.Q3);
            case 12 -> List.of(Ky.Q4, Ky.NAM);
            default -> List.of();
        };
    }

    private static void type(List<LoaiTaiLieu> types, String... cues) {
        String any = String.join("|", List.of(cues).stream().map(Pattern::quote).toList());
        DOCUMENT_TYPES.put(Pattern.compile("(?<![\\p{L}\\p{N}])(?:" + any + ")(?![\\p{L}\\p{N}])"), types);
    }
}
