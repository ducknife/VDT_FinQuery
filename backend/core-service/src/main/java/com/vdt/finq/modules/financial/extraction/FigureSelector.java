package com.vdt.finq.modules.financial.extraction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.vdt.finq.modules.document.Document;
import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;

import lombok.AllArgsConstructor;
import lombok.Getter;

// một kỳ chỉ một dòng hiệu lực; chọn nguồn có thẩm quyền nhất (cao xuống thấp):
// số trình bày lại > đính chính > báo cáo năm > bán niên > cột so sánh kỳ sau > báo cáo quý.
// số gốc bị thay thì giữ lại với trạng thái da_thay_the
public final class FigureSelector {

    private FigureSelector() {
    }

    @Getter
    @AllArgsConstructor
    public static class Selected {
        private final FigureCandidate winner;
        // số gốc bị thay (chỉ có khi winner là số trình bày lại và khác số gốc)
        private final FigureCandidate replaced;
    }

    @Getter
    public static class Selection {
        private final List<Selected> selected = new ArrayList<>();
        // các ca hai nguồn cùng bậc ưu tiên trở xuống mà khác số, để soát
        private final List<String> conflicts = new ArrayList<>();
    }

    public static Selection select(List<FigureCandidate> candidates, Map<String, Document> documents) {
        Map<String, List<FigureCandidate>> byKey = new LinkedHashMap<>();
        for (FigureCandidate c : candidates) {
            String key = c.getMaNoiBo() + "|" + c.getPeriod() + "|" + c.getPhamVi();
            byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }

        Comparator<FigureCandidate> order = Comparator
                .comparingInt((FigureCandidate c) -> rank(c, documents.get(c.getTaiLieuId())))
                .thenComparing(c -> !documents.get(c.getTaiLieuId()).isLaBanChinh())
                .thenComparing(c -> documents.get(c.getTaiLieuId()).getNgayKetThucKy(), Comparator.reverseOrder())
                .thenComparing(FigureCandidate::getTaiLieuId)
                .thenComparingInt(FigureCandidate::getTrang);

        Selection selection = new Selection();
        for (Map.Entry<String, List<FigureCandidate>> entry : byKey.entrySet()) {
            List<FigureCandidate> list = new ArrayList<>(entry.getValue());
            list.sort(order);
            FigureCandidate winner = list.get(0);
            boolean restated = rank(winner, documents.get(winner.getTaiLieuId())) == 0;

            FigureCandidate replaced = null;
            if (restated) {
                replaced = list.stream()
                        .filter(c -> rank(c, documents.get(c.getTaiLieuId())) > 0)
                        .findFirst()
                        .filter(c -> c.getGiaTri() != winner.getGiaTri())
                        .orElse(null);
            } else {
                for (FigureCandidate other : list) {
                    if (other.getGiaTri() != winner.getGiaTri()) {
                        selection.conflicts.add(entry.getKey() + ": " + winner.getTaiLieuId() + " tr." + winner.getTrang()
                                + " = " + winner.getGiaTri() + " | " + other.getTaiLieuId() + " tr." + other.getTrang()
                                + " = " + other.getGiaTri());
                        break;
                    }
                }
            }
            selection.selected.add(new Selected(winner, replaced));
        }
        return selection;
    }

    static int rank(FigureCandidate c, Document document) {
        boolean audited = document.getKy() == Ky.NAM || document.getKy() == Ky.BAN_NIEN;
        if (!c.isCurrent()) {
            if (c.isRestated()) {
                return 0;
            }
            return audited ? 4 : 6;
        }
        if (document.getLoai() == LoaiTaiLieu.DINH_CHINH) {
            return 1;
        }
        if (document.getKy() == Ky.NAM) {
            return 2;
        }
        return document.getKy() == Ky.BAN_NIEN ? 3 : 5;
    }
}
