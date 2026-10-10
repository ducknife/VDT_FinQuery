package com.vdt.finq.modules.financial.extraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vdt.finq.modules.financial.FinancialEnums.MauBieu;

import lombok.AllArgsConstructor;
import lombok.Getter;

// các phép cộng kế toán phải đúng tới từng đồng. viết theo MÃ NỘI BỘ chứ không theo mã số,
// vì TT99 đổi số mã (tổng tài sản: 270 ở TT200, 280 ở TT99) nhưng quan hệ cộng giữ nguyên.
// OCR đọc sai một chữ số ở bất kỳ dòng nào tham gia thì phép cộng lệch, cả nhóm bị loại khỏi DB
public final class BalanceIdentities {

    private BalanceIdentities() {
    }

    @Getter
    @AllArgsConstructor
    public static class Identity {
        private final Set<MauBieu> forms;
        private final String total;
        // hệ số +1 / -1 cho từng thành phần
        private final Map<String, Integer> parts;

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder(total).append(" =");
            parts.forEach((code, sign) -> s.append(sign > 0 ? " + " : " - ").append(code));
            return s.toString();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Failure {
        private final Identity identity;
        private final long expected;
        private final long actual;
    }

    private static final Set<MauBieu> B01 = Set.of(MauBieu.B01);
    private static final Set<MauBieu> B02 = Set.of(MauBieu.B02);
    private static final Set<MauBieu> B03 = Set.of(MauBieu.B03_GT, MauBieu.B03_TT);

    public static final List<Identity> ALL = List.of(
            // B01: tài sản
            plus(B01, "TIEN_VA_TUONG_DUONG_TIEN", "TIEN_M111", "TUONG_DUONG_TIEN"),
            plus(B01, "DAU_TU_TC_NGAN_HAN", "DAU_TU_NAM_GIU_DAO_HAN_NH"),
            plus(B01, "PHAI_THU_NGAN_HAN", "PHAI_THU_KHACH_HANG_NH", "TRA_TRUOC_NGUOI_BAN_NH",
                    "PHAI_THU_NGAN_HAN_KHAC", "DU_PHONG_PHAI_THU_KHO_DOI"),
            plus(B01, "HANG_TON_KHO_M140", "HANG_TON_KHO_M141", "DU_PHONG_GIAM_GIA_HTK"),
            plus(B01, "TAI_SAN_NGAN_HAN_KHAC", "CHI_PHI_CHO_PHAN_BO_NGAN_HAN", "THUE_GTGT_DUOC_KHAU_TRU",
                    "THUE_PHAI_THU_NHA_NUOC"),
            plus(B01, "TAI_SAN_NGAN_HAN", "TIEN_VA_TUONG_DUONG_TIEN", "DAU_TU_TC_NGAN_HAN", "PHAI_THU_NGAN_HAN",
                    "HANG_TON_KHO_M140", "TAI_SAN_NGAN_HAN_KHAC"),
            plus(B01, "TSCD_HUU_HINH", "TSCD_HUU_HINH_NGUYEN_GIA", "TSCD_HUU_HINH_HAO_MON"),
            plus(B01, "TSCD_VO_HINH", "TSCD_VO_HINH_NGUYEN_GIA", "TSCD_VO_HINH_HAO_MON"),
            plus(B01, "BDS_DAU_TU", "BDS_DAU_TU_NGUYEN_GIA", "BDS_DAU_TU_HAO_MON"),
            plus(B01, "TAI_SAN_CO_DINH", "TSCD_HUU_HINH", "TSCD_VO_HINH"),
            plus(B01, "TAI_SAN_DO_DANG_DH", "CHI_PHI_XDCB_DO_DANG"),
            plus(B01, "TAI_SAN_DAI_HAN_KHAC", "CHI_PHI_CHO_PHAN_BO_DAI_HAN", "TAI_SAN_THUE_TNDN_HOAN_LAI"),
            plus(B01, "TAI_SAN_DAI_HAN", "TAI_SAN_CO_DINH", "BDS_DAU_TU", "TAI_SAN_DO_DANG_DH",
                    "DAU_TU_TC_DAI_HAN", "TAI_SAN_DAI_HAN_KHAC"),
            plus(B01, "TONG_TAI_SAN", "TAI_SAN_NGAN_HAN", "TAI_SAN_DAI_HAN"),
            // B01: nguồn vốn
            plus(B01, "NO_NGAN_HAN", "PHAI_TRA_NGUOI_BAN_NH", "NGUOI_MUA_TRA_TIEN_TRUOC_NH",
                    "PHAI_TRA_CO_TUC_LOI_NHUAN", "THUE_PHAI_NOP_NHA_NUOC", "PHAI_TRA_NGUOI_LAO_DONG",
                    "CHI_PHI_PHAI_TRA_NH", "PHAI_TRA_NOI_BO_NH", "PHAI_TRA_TIEN_DO_HDXD",
                    "DOANH_THU_CHUA_THUC_HIEN_NH", "PHAI_TRA_NGAN_HAN_KHAC", "VAY_NO_THUE_TC_NH",
                    "DU_PHONG_PHAI_TRA_NH", "QUY_KHEN_THUONG_PHUC_LOI"),
            plus(B01, "NO_DAI_HAN", "PHAI_TRA_DAI_HAN_KHAC", "VAY_NO_THUE_TC_DH"),
            plus(B01, "NO_PHAI_TRA", "NO_NGAN_HAN", "NO_DAI_HAN"),
            plus(B01, "VON_CHU_SO_HUU_M410", "VON_GOP_CHU_SO_HUU", "THANG_DU_VON", "CHENH_LECH_TY_GIA",
                    "QUY_DAU_TU_PHAT_TRIEN", "LNST_CHUA_PHAN_PHOI", "LOI_ICH_CDKKS"),
            plus(B01, "TONG_NGUON_VON", "NO_PHAI_TRA", "VON_CHU_SO_HUU_M400"),
            plus(B01, "TONG_TAI_SAN", "TONG_NGUON_VON"),
            // B02
            new Identity(B02, "DOANH_THU_THUAN", Map.of("DOANH_THU_BAN_HANG", 1, "CAC_KHOAN_GIAM_TRU_DT", -1)),
            new Identity(B02, "LOI_NHUAN_GOP", Map.of("DOANH_THU_THUAN", 1, "GIA_VON_HANG_BAN", -1)),
            new Identity(B02, "LOI_NHUAN_THUAN_HDKD", Map.of("LOI_NHUAN_GOP", 1, "LAI_LO_BAN_THANH_LY_BDSDT", 1,
                    "DOANH_THU_TAI_CHINH", 1, "CHI_PHI_TAI_CHINH", -1, "LAI_LO_LIEN_DOANH_LIEN_KET", 1,
                    "CHI_PHI_BAN_HANG", -1, "CHI_PHI_QUAN_LY", -1)),
            new Identity(B02, "LOI_NHUAN_KHAC", Map.of("THU_NHAP_KHAC", 1, "CHI_PHI_KHAC", -1)),
            plus(B02, "LOI_NHUAN_TRUOC_THUE", "LOI_NHUAN_THUAN_HDKD", "LOI_NHUAN_KHAC"),
            new Identity(B02, "LOI_NHUAN_SAU_THUE", Map.of("LOI_NHUAN_TRUOC_THUE", 1,
                    "CHI_PHI_THUE_TNDN_HIEN_HANH", -1, "CHI_PHI_THUE_TNDN_HOAN_LAI", -1)),
            plus(B02, "LOI_NHUAN_SAU_THUE", "LNST_CONG_TY_ME", "LNST_CDKKS"),
            // B03: số âm đã mang dấu trong báo cáo nên chỉ việc cộng
            plus(Set.of(MauBieu.B03_GT), "LCTT_LN_TRUOC_THAY_DOI_VLD", "LCTT_LOI_NHUAN_TRUOC_THUE", "LCTT_KHAU_HAO",
                    "LCTT_DU_PHONG", "LCTT_LAI_LO_TY_GIA", "LCTT_LAI_LO_DAU_TU", "LCTT_CHI_PHI_DI_VAY",
                    "LCTT_DIEU_CHINH_KHAC"),
            plus(Set.of(MauBieu.B03_GT), "LCTT_THUAN_HDKD", "LCTT_LN_TRUOC_THAY_DOI_VLD", "LCTT_TANG_GIAM_PHAI_THU",
                    "LCTT_TANG_GIAM_HTK", "LCTT_TANG_GIAM_PHAI_TRA", "LCTT_TANG_GIAM_CHI_PHI_CHO_PHAN_BO",
                    "LCTT_CHI_PHI_DI_VAY_DA_TRA", "LCTT_THUE_TNDN_DA_NOP", "LCTT_TIEN_THU_KHAC_HDKD",
                    "LCTT_TIEN_CHI_KHAC_HDKD"),
            plus(Set.of(MauBieu.B03_TT), "LCTT_THUAN_HDKD", "LCTT_TT_THU_BAN_HANG", "LCTT_TT_CHI_NGUOI_CUNG_CAP",
                    "LCTT_TT_CHI_NGUOI_LAO_DONG", "LCTT_CHI_PHI_DI_VAY_DA_TRA", "LCTT_THUE_TNDN_DA_NOP",
                    "LCTT_TIEN_THU_KHAC_HDKD", "LCTT_TIEN_CHI_KHAC_HDKD"),
            plus(B03, "LCTT_THUAN_HDDT", "LCTT_CHI_MUA_TSCD", "LCTT_THU_THANH_LY_TSCD", "LCTT_CHI_CHO_VAY",
                    "LCTT_THU_HOI_CHO_VAY", "LCTT_CHI_GOP_VON", "LCTT_THU_HOI_GOP_VON", "LCTT_THU_LAI_CO_TUC"),
            plus(B03, "LCTT_THUAN_HDTC", "LCTT_THU_PHAT_HANH_CP", "LCTT_THU_DI_VAY", "LCTT_TRA_NO_GOC_VAY",
                    "LCTT_CO_TUC_DA_TRA"),
            plus(B03, "LCTT_THUAN_TRONG_KY", "LCTT_THUAN_HDKD", "LCTT_THUAN_HDDT", "LCTT_THUAN_HDTC"),
            plus(B03, "TIEN_CUOI_KY", "TIEN_DAU_KY", "LCTT_THUAN_TRONG_KY", "LCTT_ANH_HUONG_TY_GIA"));

    // kiểm các phép cộng trên giá trị của MỘT cột (mã nội bộ -> số).
    // chỉ kiểm khi có tổng và ít nhất một thành phần; thành phần vắng mặt tính là 0
    // (báo cáo bỏ dòng bằng 0; còn dòng bị OCR làm rơi thì phép cộng sẽ lệch và lộ ra)
    public static List<Failure> check(MauBieu form, Map<String, Long> values) {
        List<Failure> failures = new ArrayList<>();
        for (Identity identity : ALL) {
            if (!identity.getForms().contains(form) || !values.containsKey(identity.getTotal())) {
                continue;
            }
            long sum = 0;
            boolean anyPart = false;
            for (Map.Entry<String, Integer> part : identity.getParts().entrySet()) {
                Long v = values.get(part.getKey());
                if (v != null) {
                    sum += part.getValue() * v;
                    anyPart = true;
                }
            }
            long total = values.get(identity.getTotal());
            if (anyPart && sum != total) {
                failures.add(new Failure(identity, sum, total));
            }
        }
        return failures;
    }

    private static Identity plus(Set<MauBieu> forms, String total, String... parts) {
        Map<String, Integer> map = new java.util.LinkedHashMap<>();
        for (String part : parts) {
            map.put(part, 1);
        }
        return new Identity(forms, total, map);
    }
}
