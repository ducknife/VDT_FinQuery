package com.vdt.finq.modules.evaluation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vdt.finq.modules.agent.tools.financial.support.VnNumber;

// chấm câu trả lời nhánh số liệu theo eval_dbquery.yml: "đúng và đủ số được hỏi" (như paper FinAI).
// chỉ là lượt chấm đầu bằng máy; người làm đề tài soát lại từng câu
public final class DbAnswerScorer {

    // số tiền đồng (>= 2 nhóm nghìn) và số tỷ đồng
    private static final Pattern DONG = Pattern.compile("(?<![\\d.,])\\d{1,3}(?:\\.\\d{3}){2,}(?![\\d])");
    private static final Pattern TY = Pattern.compile("(?<![\\d.,])(\\d{1,3}(?:\\.\\d{3})*(?:,\\d+)?)\\s*t[ỷỉ]");

    private DbAnswerScorer() {
    }

    public record Verdict(boolean correct, String reason) {
    }

    public static Verdict score(String answer, Map<String, Object> cham) {
        String a = answer == null ? "" : Normalizer.normalize(answer, Normalizer.Form.NFC).replace(' ', ' ');
        String lower = a.toLowerCase();
        String kieu = String.valueOf(cham.get("kieu"));
        return switch (kieu) {
            case "so_dong" -> money(a, lower, ((Number) cham.get("gia_tri")).longValue(), Boolean.TRUE.equals(cham.get("am")));
            case "phan_tram" -> percent(a, lower, String.valueOf(cham.get("gia_tri")), (String) cham.get("huong"));
            case "lan" -> has(a, String.valueOf(cham.get("gia_tri")))
                    ? new Verdict(true, "có " + cham.get("gia_tri") + " lần")
                    : new Verdict(false, "thiếu " + cham.get("gia_tri") + " lần");
            case "tu_choi" -> refusal(a, allowed(cham.get("so_duoc_phep")));
            default -> new Verdict(false, "kiểu chấm lạ: " + kieu);
        };
    }

    private static Verdict money(String a, String lower, long value, boolean negative) {
        String dong = VnNumber.dong(Math.abs(value));
        String ty = VnNumber.billion(Math.abs(value));
        boolean exact = has(a, dong);
        boolean billion = Pattern.compile("(?<![\\d.,])" + Pattern.quote(ty) + "\\s*t[ỷỉ]").matcher(a).find();
        if (!exact && !billion) {
            return new Verdict(false, "thiếu " + dong + " (hoặc " + ty + " tỷ)");
        }
        if (negative && !(a.contains("-" + dong) || a.contains("−" + dong) || a.contains("(" + dong + ")")
                || a.contains("-" + ty) || lower.contains("âm"))) {
            return new Verdict(false, "đúng độ lớn nhưng không nói là số âm");
        }
        return new Verdict(true, exact ? "có số đồng chính xác" : "có số tỷ đồng làm tròn");
    }

    private static Verdict percent(String a, String lower, String value, String direction) {
        boolean found = Pattern.compile("(?<![\\d.,])" + Pattern.quote(value) + "\\s*%").matcher(a).find();
        if (!found) {
            return new Verdict(false, "thiếu " + value + "%");
        }
        if ("tang".equals(direction) && !lower.contains("tăng")) {
            return new Verdict(false, "có " + value + "% nhưng không nói tăng");
        }
        if ("giam".equals(direction) && !lower.contains("giảm") && !a.contains("-" + value)) {
            return new Verdict(false, "có " + value + "% nhưng không nói giảm");
        }
        return new Verdict(true, "có " + value + "%");
    }

    // từ chối đúng: không đưa ra số tiền nào ngoài các số được phép
    private static Verdict refusal(String a, List<String> allowed) {
        if (a.isBlank()) {
            return new Verdict(false, "không trả lời");
        }
        List<String> invented = new ArrayList<>();
        Matcher m = DONG.matcher(a);
        while (m.find()) {
            if (!allowed.contains(m.group())) {
                invented.add(m.group());
            }
        }
        Matcher t = TY.matcher(a);
        while (t.find()) {
            if (!allowed.contains(t.group(1))) {
                invented.add(t.group(1) + " tỷ");
            }
        }
        return invented.isEmpty() ? new Verdict(true, "không đưa số cho kỳ không có")
                : new Verdict(false, "đưa ra số không được phép: " + invented);
    }

    private static List<String> allowed(Object list) {
        List<String> out = new ArrayList<>();
        if (list instanceof List<?> l) {
            for (Object o : l) {
                long v = ((Number) o).longValue();
                out.add(VnNumber.dong(v));
                out.add(VnNumber.billion(v));
            }
        }
        return out;
    }

    private static boolean has(String a, String number) {
        return Pattern.compile("(?<![\\d.,])" + Pattern.quote(number) + "(?![\\d])").matcher(a).find();
    }
}
