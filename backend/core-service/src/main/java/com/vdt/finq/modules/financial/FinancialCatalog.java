package com.vdt.finq.modules.financial;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.vdt.finq.modules.financial.FinancialEnums.LoaiSo;
import com.vdt.finq.modules.financial.FinancialEnums.MauBieu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// từ điển chỉ tiêu và bảng ánh xạ mã số -> mã nội bộ, đọc một lần mỗi lượt trích
@Component
@RequiredArgsConstructor
public class FinancialCatalog {

    private final JdbcTemplate jdbcTemplate;

    @Getter
    @AllArgsConstructor
    public static class Mapping {
        private final String maNoiBo;
        private final String tenTrenBaoCao;
    }

    @Getter
    @AllArgsConstructor
    public static class Item {
        private final String maNoiBo;
        private final LoaiSo loaiSo;
        // 1 nếu bản chất là số âm: dự phòng, hao mòn, dòng tiền chi
        private final boolean dauAmTuNhien;
    }

    // ảnh chụp hai bảng tại một thời điểm
    public static class Snapshot {
        private final Map<String, Mapping> mappings;
        private final Map<String, Item> items;

        Snapshot(Map<String, Mapping> mappings, Map<String, Item> items) {
            this.mappings = mappings;
            this.items = items;
        }

        public Mapping mapping(String cheDo, MauBieu mauBieu, String ma) {
            return mappings.get(key(cheDo, mauBieu.getValue(), ma));
        }

        public Item item(String maNoiBo) {
            return items.get(maNoiBo);
        }
    }

    public Snapshot load() {
        Map<String, Mapping> mappings = new HashMap<>();
        jdbcTemplate.query("SELECT che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao FROM financial_item_mappings",
                rs -> {
                    mappings.put(key(rs.getString("che_do"), rs.getString("mau_bieu"), rs.getString("ma")),
                            new Mapping(rs.getString("ma_noi_bo"), rs.getString("ten_tren_bao_cao")));
                });
        Map<String, Item> items = new HashMap<>();
        jdbcTemplate.query("SELECT ma_noi_bo, loai_so, dau_am_tu_nhien FROM financial_items", rs -> {
            String loaiSo = rs.getString("loai_so");
            items.put(rs.getString("ma_noi_bo"), new Item(rs.getString("ma_noi_bo"),
                    LoaiSo.THOI_DIEM.getValue().equals(loaiSo) ? LoaiSo.THOI_DIEM : LoaiSo.THOI_KY,
                    rs.getBoolean("dau_am_tu_nhien")));
        });
        return new Snapshot(mappings, items);
    }

    private static String key(String cheDo, String mauBieu, String ma) {
        return cheDo + "|" + mauBieu + "|" + ma;
    }
}
