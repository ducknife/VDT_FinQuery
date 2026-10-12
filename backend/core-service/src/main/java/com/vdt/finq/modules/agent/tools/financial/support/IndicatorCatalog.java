package com.vdt.finq.modules.agent.tools.financial.support;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vdt.finq.modules.agent.tools.financial.support.FigureQueryRepository.ItemRow;

import lombok.RequiredArgsConstructor;

// 123 mã chỉ tiêu: kiểm mã LLM điền và sinh danh sách gửi một lần trong system prompt
@Component
@RequiredArgsConstructor
public class IndicatorCatalog {

    private final FigureQueryRepository repository;
    private volatile Map<String, ItemRow> items;

    private Map<String, ItemRow> items() {
        if (items == null) {
            synchronized (this) {
                if (items == null) {
                    Map<String, ItemRow> m = new LinkedHashMap<>();
                    repository.items().forEach(i -> m.put(i.code(), i));
                    items = m;
                }
            }
        }
        return items;
    }

    public ItemRow get(String code) {
        return code == null ? null : items().get(code.strip().toUpperCase(Locale.ROOT));
    }

    // mã sai: gợi ý vài mã gần nhất theo số từ trùng (không tự thay, LLM phải chọn lại)
    public List<String> suggest(String code) {
        List<String> words = Arrays.asList((code == null ? "" : code.toUpperCase(Locale.ROOT)).split("[_\\s]+"));
        return items().keySet().stream()
                .sorted(Comparator.comparingLong((String k) -> -Arrays.stream(k.split("_")).filter(words::contains).count()))
                .limit(5).toList();
    }

    // khối gửi trong system prompt của agent: mã | tên | P (thời điểm) / F (thời kỳ)
    public String promptSection() {
        StringBuilder s = new StringBuilder("""

                INDICATOR CODES (the only valid values for "indicator"; P = point in time, balance sheet;
                F = flow over a period, income statement and cash flow). Pick the code whose name matches what is
                asked; NEVER substitute a similar indicator. If nothing matches, use search_documents.
                """);
        for (ItemRow i : items().values()) {
            s.append(i.code()).append(" | ").append(i.name()).append(" | ").append(i.pointInTime() ? "P" : "F");
            if (i.replacedBy() != null) {
                s.append(" | replaced by ").append(i.replacedBy()).append(" in TT99");
            }
            s.append('\n');
        }
        return s.toString();
    }
}
