package com.vdt.finq.modules.dbquery.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vdt.finq.modules.agent.evidence.Evidence;
import com.vdt.finq.modules.agent.evidence.EvidenceStore;
import com.vdt.finq.modules.dbquery.support.FigureQueryRepository.ItemRow;
import com.vdt.finq.modules.dbquery.support.FigureQueryRepository.Row;

import lombok.RequiredArgsConstructor;

// tra một số (mã, kỳ, phạm vi) thành bằng chứng D#, kiểm mã và kỳ trước khi chạm DB.
// quý N của chỉ tiêu chỉ có luỹ kế (lưu chuyển tiền tệ) = luỹ kế N - luỹ kế N-1
@Component
@RequiredArgsConstructor
public class FigureLookup {

    private static final int FIRST_YEAR = 2017;

    private final FigureQueryRepository repository;
    private final IndicatorCatalog catalog;
    private final Map<Scope, LocalDate> latest = new EnumMap<>(Scope.class);

    // kết quả tra: có số thì evidence + value, không thì error (câu ngắn tiếng Anh gửi lại cho LLM)
    public record Fetched(Evidence evidence, BigDecimal value, String label, String error) {
        public boolean ok() {
            return error == null;
        }

        static Fetched fail(String error) {
            return new Fetched(null, null, null, error);
        }
    }

    public LocalDate latest(Scope scope) {
        return latest.computeIfAbsent(scope, s -> repository.latest(s.getValue()));
    }

    public Fetched fetch(EvidenceStore store, String code, ReportPeriod p, Scope scope) {
        ItemRow item = catalog.get(code);
        if (item == null) {
            return Fetched.fail("UNKNOWN_INDICATOR \"" + code + "\": not a valid code. Closest codes: "
                    + catalog.suggest(code) + ". Do not substitute a similar indicator; if none matches, use search_documents.");
        }
        LocalDate last = latest(scope);
        String label = item.name() + ", " + scope.getLabel() + ", " + (item.pointInTime() ? p.pointLabel() : p.flowLabel());
        if (p.year() < FIRST_YEAR) {
            Evidence note = notAvailable(store, item, p, scope, label + ": không có trong kho; kho số liệu bắt đầu từ 31/12/2017.");
            return Fetched.fail("[" + note.getId() + "] NOT_AVAILABLE " + item.code() + " " + p.year()
                    + ": the figures database starts at 31/12/2017.");
        }
        if (last != null && p.end().isAfter(last)) {
            Evidence note = notAvailable(store, item, p, scope, label + ": chưa công bố; kỳ mới nhất trong kho là "
                    + ReportPeriod.date(last) + ".");
            return Fetched.fail("[" + note.getId() + "] NOT_AVAILABLE " + item.code() + " " + p.period() + "/" + p.year()
                    + " (" + scope.getLabel() + "): the latest period in the database is " + ReportPeriod.date(last)
                    + ". The requested period has not been reported yet. Do not extrapolate.");
        }
        LocalDate from = item.pointInTime() ? p.end() : p.flowStart();
        LocalDate to = p.end();
        String key = item.code() + "|" + scope.getValue() + "|" + from + "|" + to;

        List<Row> rows = repository.find(item.code(), scope.getValue(), from, to);
        Row current = rows.stream().filter(Row::current).findFirst().orElse(null);
        if (current != null) {
            Evidence e = store.figure(key);
            if (e == null) {
                e = store.addFigure(key, current.taiLieuId(), current.tenHienThi(), current.trang(),
                        content(item, label, current, rows));
            }
            return new Fetched(e, BigDecimal.valueOf(current.value()), label, null);
        }
        if (!item.pointInTime() && p.isSingleLaterQuarter()) {
            return derivedQuarter(store, item, p, scope, label, key);
        }
        String have = repository.available(item.code(), scope.getValue()).stream()
                .map(d -> d[0].equals(d[1]) ? ReportPeriod.date(d[1]) : ReportPeriod.date(d[0]) + "–" + ReportPeriod.date(d[1]))
                .collect(Collectors.joining(", "));
        String replaced = item.replacedBy() == null ? ""
                : " This code was replaced by " + item.replacedBy() + " under TT99; suggest that code to the user, do not substitute silently.";
        return Fetched.fail("NOT_FOUND " + item.code() + " (" + label + "). Latest periods available for this indicator: "
                + (have.isEmpty() ? "none" : have) + "." + replaced);
    }

    // quý N = luỹ kế đến N - luỹ kế đến N-1 (lưu chuyển tiền tệ chỉ công bố số luỹ kế)
    private Fetched derivedQuarter(EvidenceStore store, ItemRow item, ReportPeriod p, Scope scope, String label, String key) {
        Fetched to = fetch(store, item.code(), p.cumulativeToEnd(), scope);
        Fetched before = fetch(store, item.code(), p.cumulativeBefore(), scope);
        if (!to.ok() || !before.ok()) {
            return Fetched.fail("NOT_FOUND " + item.code() + " (" + label + "): no single-quarter figure, and the "
                    + "cumulative figures needed to derive it are missing: " + (to.ok() ? before.error() : to.error()));
        }
        BigDecimal value = to.value().subtract(before.value());
        Evidence e = store.figure(key);
        if (e == null) {
            e = store.addFigure(key, null, null, 0, label + " (" + item.code() + "): " + VnNumber.money(value)
                    + ". Java tính = [" + to.evidence().getId() + "] " + VnNumber.dong(to.value().longValue()) + " − ["
                    + before.evidence().getId() + "] " + VnNumber.dong(before.value().longValue())
                    + " (báo cáo chỉ công bố số luỹ kế, số riêng quý suy ra từ hai kỳ luỹ kế).");
        }
        return new Fetched(e, value, label, null);
    }

    // kỳ chưa công bố / ngoài kho cũng là bằng chứng: để bước trả lời nói đúng lý do thay vì "không tìm thấy"
    private static Evidence notAvailable(EvidenceStore store, ItemRow item, ReportPeriod p, Scope scope, String content) {
        String key = "NA|" + item.code() + "|" + scope.getValue() + "|" + p.period() + "|" + p.year();
        Evidence e = store.figure(key);
        return e != null ? e : store.addFigure(key, null, null, 0, content);
    }

    private static String content(ItemRow item, String label, Row current, List<Row> rows) {
        StringBuilder s = new StringBuilder(label).append(" (").append(item.code()).append("): ")
                .append(VnNumber.money(current.value())).append(". Nguồn: ")
                .append(current.tenHienThi() == null ? current.taiLieuId() : current.tenHienThi())
                .append(", trang ").append(current.trang()).append('.');
        for (Row old : rows) {
            if (!old.current()) {
                s.append(" Đây là số đã trình bày lại").append(current.reason() == null ? "" : " (" + current.reason() + ")")
                        .append("; số công bố ban đầu: ").append(VnNumber.money(old.value())).append(" (")
                        .append(old.tenHienThi() == null ? old.taiLieuId() : old.tenHienThi()).append(", trang ")
                        .append(old.trang()).append(").");
            }
        }
        return s.toString();
    }
}
