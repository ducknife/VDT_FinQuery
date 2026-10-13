package com.vdt.finq.modules.rag.generation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.vdt.finq.modules.agent.evidence.Evidence;
import com.vdt.finq.modules.dbquery.support.VnNumber;

// phép tính model khai báo ở bước trả lời (số lấy từ C# hoặc D#): Java kiểm số có nguyên văn trong bằng chứng,
// quy đơn vị, tính BigDecimal rồi điền vào chỗ {{K#}}. model không tự tính (QĐ-041)
public final class EvidenceCalculator {

    public record Calculation(String id, String expression, String result, boolean ok) {
    }

    private record Operand(String evidenceId, String raw, BigDecimal value, String unit) {
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(K\\d+)\\s*}}");
    private static final Pattern TREND = Pattern.compile("(tăng|giảm) ");
    private static final Pattern TREND_BEFORE = Pattern.compile("(^|[^\\p{L}])(?:tăng|giảm|Tăng|Giảm)\\s*$");
    private static final Map<String, BigDecimal> MONEY = Map.of(
            "đồng", BigDecimal.ONE,
            "nghìn đồng", BigDecimal.valueOf(1_000L),
            "triệu đồng", BigDecimal.valueOf(1_000_000L),
            "tỷ đồng", BigDecimal.valueOf(1_000_000_000L));

    private EvidenceCalculator() {
    }

    public static List<Calculation> run(JsonNode requests, List<Evidence> evidence) {
        List<Calculation> out = new ArrayList<>();
        for (JsonNode r : requests) {
            String id = r.path("id").asText("").strip();
            String op = r.path("op").asText("").strip();
            try {
                Operand a = operand(r.path("a"), evidence);
                Operand b = operand(r.path("b"), evidence);
                out.add(compute(id, op, a, b));
            } catch (IllegalArgumentException e) {
                out.add(new Calculation(id, op, "không tính được: " + e.getMessage(), false));
            }
        }
        return out;
    }

    // thay {{K#}} bằng kết quả; chỗ đặt không có phép tính tương ứng thì ghi rõ
    public static String fill(String answer, List<Calculation> calculations) {
        if (answer == null) {
            return "";
        }
        Matcher m = PLACEHOLDER.matcher(answer);
        StringBuilder s = new StringBuilder();
        while (m.find()) {
            String id = m.group(1);
            String value = calculations.stream().filter(c -> c.id().equals(id)).findFirst()
                    .map(c -> c.ok() ? c.result() : "(" + c.result() + ")")
                    .orElse("(không tính được: thiếu phép tính " + id + ")");
            m.appendReplacement(s, "");
            // "tăng {{K2}}" mà kết quả đã là "tăng 35,04%": bỏ chữ tăng/giảm model viết thừa phía trước
            Matcher before = TREND_BEFORE.matcher(s);
            if (TREND.matcher(value).lookingAt() && before.find()) {
                String space = before.group(1);
                s.setLength(before.start());
                s.append(space);
            }
            s.append(value);
        }
        m.appendTail(s);
        return s.toString();
    }

    private static Operand operand(JsonNode node, List<Evidence> evidence) {
        String evidenceId = node.path("id").asText("").strip();
        String raw = node.path("value").asText("").strip();
        String unit = node.path("unit").asText("").strip().toLowerCase(Locale.ROOT);
        Evidence e = evidence.stream().filter(x -> x.getId().equals(evidenceId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("không có bằng chứng " + evidenceId));
        if (raw.isEmpty() || !squash(e.getContent()).contains(squash(raw))) {
            throw new IllegalArgumentException("số \"" + raw + "\" không có trong [" + evidenceId + "]");
        }
        return new Operand(evidenceId, raw, parse(raw), unit);
    }

    private static Calculation compute(String id, String op, Operand a, Operand b) {
        boolean money = MONEY.containsKey(a.unit()) && MONEY.containsKey(b.unit());
        if (!money && !a.unit().equals(b.unit())) {
            throw new IllegalArgumentException("khác đơn vị (" + a.unit() + ", " + b.unit() + ")");
        }
        BigDecimal x = money ? a.value().multiply(MONEY.get(a.unit())) : a.value();
        BigDecimal y = money ? b.value().multiply(MONEY.get(b.unit())) : b.value();
        String expression;
        String result;
        switch (op) {
            case "difference" -> {
                expression = describe(a) + " − " + describe(b);
                result = amount(x.subtract(y), money, a.unit());
            }
            case "sum" -> {
                expression = describe(a) + " + " + describe(b);
                result = amount(x.add(y), money, a.unit());
            }
            case "percent_change" -> {
                requireNonZero(y);
                expression = "(" + describe(a) + " − " + describe(b) + ") / " + describe(b);
                BigDecimal change = x.subtract(y).divide(y.abs(), MathContext.DECIMAL64);
                result = (change.signum() < 0 ? "giảm " : "tăng ") + VnNumber.percent(change.abs());
            }
            case "ratio" -> {
                requireNonZero(y);
                expression = describe(a) + " / " + describe(b);
                BigDecimal ratio = x.divide(y, MathContext.DECIMAL64);
                result = VnNumber.times(ratio);
            }
            default -> throw new IllegalArgumentException("phép tính \"" + op + "\" không hỗ trợ");
        }
        return new Calculation(id, expression, result, true);
    }

    // tiền: số đồng chính xác kèm số tỷ; hiệu hai tỷ lệ % là điểm phần trăm; số đếm giữ đơn vị gốc
    private static String amount(BigDecimal v, boolean money, String unit) {
        if (money) {
            return VnNumber.money(v);
        }
        String n = plain(v);
        return "%".equals(unit) ? n + " điểm phần trăm" : (unit.isEmpty() ? n : n + " " + unit);
    }

    private static String plain(BigDecimal v) {
        BigDecimal r = v.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        String s = r.abs().toPlainString();
        String[] parts = s.split("\\.");
        StringBuilder grouped = new StringBuilder(parts[0]);
        for (int i = grouped.length() - 3; i > 0; i -= 3) {
            grouped.insert(i, '.');
        }
        return (r.signum() < 0 ? "-" : "") + grouped + (parts.length > 1 ? "," + parts[1] : "");
    }

    private static String describe(Operand o) {
        return "[" + o.evidenceId() + "] " + o.raw() + (o.unit().isEmpty() || o.raw().endsWith(o.unit()) ? "" : " " + o.unit());
    }

    private static void requireNonZero(BigDecimal y) {
        if (y.signum() == 0) {
            throw new IllegalArgumentException("chia cho 0");
        }
    }

    // "1.281.095.330.000", "1,281,095,330,000", "125,05", "(2.520.172.619)", "-3,5", "10,55%" -> BigDecimal
    static BigDecimal parse(String raw) {
        String s = raw.replaceAll("[\\s%]", "");
        boolean negative = s.startsWith("(") && s.endsWith(")") || s.startsWith("-") || s.startsWith("−");
        s = s.replaceAll("[()\\-−]", "");
        if (!s.matches("[0-9.,]+") || !s.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("\"" + raw + "\" không phải số");
        }
        int dot = s.lastIndexOf('.');
        int comma = s.lastIndexOf(',');
        String normalized;
        if (dot >= 0 && comma >= 0) {
            // dấu xuất hiện sau cùng là dấu thập phân
            normalized = dot > comma ? s.replace(",", "") : s.replace(".", "").replace(',', '.');
        } else if (comma >= 0) {
            // chỉ có phẩy: nhóm 3 chữ số (có thể nhiều lần) là phân cách nghìn kiểu Anh, còn lại là thập phân
            normalized = s.matches("\\d{1,3}(,\\d{3})+") && (s.indexOf(',') != comma || s.length() - comma - 1 == 3)
                    ? s.replace(",", "") : s.replace(',', '.');
        } else if (dot >= 0) {
            // chỉ có chấm: nhóm 3 chữ số là phân cách nghìn kiểu Việt, còn lại là thập phân kiểu Anh
            normalized = s.matches("\\d{1,3}(\\.\\d{3})+") ? s.replace(".", "") : s;
        } else {
            normalized = s;
        }
        BigDecimal v = new BigDecimal(normalized);
        return negative ? v.negate() : v;
    }

    // so khớp số với bằng chứng: bỏ khoảng trắng để "1.143.858.790.000" khớp cả khi bảng xuống dòng
    private static String squash(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "");
    }
}
