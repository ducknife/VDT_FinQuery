package com.vdt.finq.modules.rag.generation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnswerFieldStreamerTest {

    // nạp từng mẩu như stream thật, ghép phần chữ trả về
    private static String run(String... chunks) {
        AnswerFieldStreamer s = new AnswerFieldStreamer();
        StringBuilder out = new StringBuilder();
        for (String c : chunks) {
            out.append(s.feed(c));
        }
        return out.toString();
    }

    @Test
    void layDungTruongAnswer() {
        String json = "{\"status\": \"answered\", \"answer\": \"Doanh thu 2024 là 12.609,52 tỷ đồng [D1].\", \"citations\": []}";
        assertThat(run(json)).isEqualTo("Doanh thu 2024 là 12.609,52 tỷ đồng [D1].");
    }

    @Test
    void catNhoTungKyTuVanDung() {
        String json = "{\"status\":\"partial\",\"answer\":\"Dòng 1\\nCó \\\"ngoặc\\\" và \\u0111\",\"missing\":\"\"}";
        String[] chunks = json.split("");
        assertThat(run(chunks)).isEqualTo("Dòng 1\nCó \"ngoặc\" và đ");
    }

    // lỗi thật lần chạy SSE đầu tiên (13/10): sau khi đọc xong answer, các mẩu citations và "}" bị bỏ
    @Test
    void giuDuJsonSauKhiHetAnswer() {
        String json = "{\"status\":\"answered\",\"answer\":\"Có [D1].\",\"citations\":[{\"id\":\"D1\",\"quote\":\"x\"}],\"missing\":\"\"}";
        AnswerFieldStreamer s = new AnswerFieldStreamer();
        for (String c : json.split("(?<=\\G.{5})")) {
            s.feed(c);
        }
        assertThat(s.raw()).isEqualTo(json);
    }

    @Test
    void khongCoAnswerThiRong() {
        assertThat(run("{\"status\": \"not_found\", \"missing\": \"x\"}")).isEmpty();
    }
}
