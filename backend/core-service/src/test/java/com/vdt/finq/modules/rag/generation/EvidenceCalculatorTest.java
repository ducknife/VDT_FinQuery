package com.vdt.finq.modules.rag.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.modules.agent.evidence.Evidence;

class EvidenceCalculatorTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final List<Evidence> EVIDENCE = List.of(
            new Evidence("D2", Evidence.Kind.FIGURE, "CTR-BCTC", "BCTC Q3 2025", 6, 0,
                    "Lợi nhuận sau thuế, hợp nhất, quý 3/2025: 168.940.075.230 đồng (≈ 168,94 tỷ đồng).", null),
            new Evidence("C1", Evidence.Kind.CHUNK, "CTR-BCTC-Q2", "BCTC Q2 2024", 6, 0,
                    "| 60 | Lợi nhuận sau thuế TNDN | 125.050.312.483 | 110.000.000.000 |", null),
            new Evidence("C2", Evidence.Kind.CHUNK, "CTR-CBTT", "Cổ tức", 5, 0,
                    "Cổ tức năm 2023 bằng tiền: 27,2%. Cổ tức năm 2024 bằng tiền: 21,5%.", null),
            new Evidence("C3", Evidence.Kind.CHUNK, "CTR-EN", "FS", 9, 0,
                    "Other profit (2,520,172,619) and charter capital 1,143,858,790,000 VND", null));

    private static JsonNode calc(String json) throws Exception {
        return JSON.readTree(json);
    }

    @Test
    void chenhLechGiuaSoDbVaSoTaiLieu() throws Exception {
        var out = EvidenceCalculator.run(calc("""
                [{"id":"K1","op":"difference","a":{"id":"D2","value":"168.940.075.230","unit":"đồng"},
                  "b":{"id":"C1","value":"125.050.312.483","unit":"đồng"}}]"""), EVIDENCE);
        assertThat(out.get(0).ok()).isTrue();
        assertThat(out.get(0).result()).isEqualTo("43.889.762.747 đồng (≈ 43,89 tỷ đồng)");
        assertThat(EvidenceCalculator.fill("Q3/2025 cao hơn {{K1}} [D2][C1].", out))
                .isEqualTo("Q3/2025 cao hơn 43.889.762.747 đồng (≈ 43,89 tỷ đồng) [D2][C1].");
    }

    @Test
    void quyDoiDonViVaPhanTramThayDoi() throws Exception {
        var out = EvidenceCalculator.run(calc("""
                [{"id":"K1","op":"percent_change","a":{"id":"D2","value":"168,94","unit":"tỷ đồng"},
                  "b":{"id":"C1","value":"125.050.312.483","unit":"đồng"}}]"""), EVIDENCE);
        assertThat(out.get(0).result()).isEqualTo("tăng 35,10%");
        // model viết thừa "tăng" trước chỗ đặt: không thành "tăng tăng"
        assertThat(EvidenceCalculator.fill("tương ứng tăng {{K1}} [D2][C1]", out)).isEqualTo("tương ứng tăng 35,10% [D2][C1]");
        assertThat(EvidenceCalculator.fill("tương ứng {{K1}}", out)).isEqualTo("tương ứng tăng 35,10%");
        // chữ "tăng" thừa nằm ngay sau dấu ngoặc
        assertThat(EvidenceCalculator.fill("cao hơn (tăng {{K1}})", out)).isEqualTo("cao hơn (tăng 35,10%)");
    }

    @Test
    void hieuHaiTyLeLaDiemPhanTram() throws Exception {
        var out = EvidenceCalculator.run(calc("""
                [{"id":"K1","op":"difference","a":{"id":"C2","value":"21,5","unit":"%"},
                  "b":{"id":"C2","value":"27,2","unit":"%"}}]"""), EVIDENCE);
        assertThat(out.get(0).result()).isEqualTo("-5,7 điểm phần trăm");
    }

    @Test
    void soKhongCoTrongBangChungThiKhongTinh() throws Exception {
        var out = EvidenceCalculator.run(calc("""
                [{"id":"K1","op":"difference","a":{"id":"D2","value":"170.000.000.000","unit":"đồng"},
                  "b":{"id":"C1","value":"125.050.312.483","unit":"đồng"}}]"""), EVIDENCE);
        assertThat(out.get(0).ok()).isFalse();
        assertThat(EvidenceCalculator.fill("cao hơn {{K1}}", out)).contains("không tính được").contains("[D2]");
        assertThat(EvidenceCalculator.fill("cao hơn {{K2}}", out)).contains("thiếu phép tính K2");
    }

    @Test
    void khacDonViDemThiKhongTinh() throws Exception {
        var out = EvidenceCalculator.run(calc("""
                [{"id":"K1","op":"ratio","a":{"id":"C2","value":"27,2","unit":"%"},
                  "b":{"id":"C1","value":"125.050.312.483","unit":"đồng"}}]"""), EVIDENCE);
        assertThat(out.get(0).ok()).isFalse();
    }

    @Test
    void docSoKieuVietKieuAnhVaSoAm() {
        assertThat(EvidenceCalculator.parse("1.281.095.330.000")).isEqualByComparingTo("1281095330000");
        assertThat(EvidenceCalculator.parse("1,143,858,790,000")).isEqualByComparingTo("1143858790000");
        assertThat(EvidenceCalculator.parse("(2,520,172,619)")).isEqualByComparingTo("-2520172619");
        assertThat(EvidenceCalculator.parse("(2.520.172.619)")).isEqualByComparingTo("-2520172619");
        assertThat(EvidenceCalculator.parse("125,05")).isEqualByComparingTo("125.05");
        assertThat(EvidenceCalculator.parse("12.609,52")).isEqualByComparingTo("12609.52");
        assertThat(EvidenceCalculator.parse("10,55%")).isEqualByComparingTo(new BigDecimal("10.55"));
        assertThat(EvidenceCalculator.parse("1,234.5")).isEqualByComparingTo("1234.5");
    }

    @Test
    void soAmTrongNgoacTuBangTiengAnh() throws Exception {
        var out = EvidenceCalculator.run(calc("""
                [{"id":"K1","op":"sum","a":{"id":"C3","value":"1,143,858,790,000","unit":"đồng"},
                  "b":{"id":"C3","value":"(2,520,172,619)","unit":"đồng"}}]"""), EVIDENCE);
        assertThat(out.get(0).result()).isEqualTo("1.141.338.617.381 đồng (≈ 1.141,34 tỷ đồng)");
    }
}
