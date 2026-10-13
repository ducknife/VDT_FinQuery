package com.vdt.finq.modules.rag.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AnswerJsonParseTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String BODY = "{\"status\":\"answered\",\"answer\":\"cao hơn {{K1}} [C40]\",\"citations\":[]}";

    @Test
    void jsonChuan() throws Exception {
        assertThat(AnswerGenerator.parseJson(JSON, BODY).path("answer").asText()).isEqualTo("cao hơn {{K1}} [C40]");
    }

    @Test
    void bocCodeFenceVaChuThua() throws Exception {
        String s = "Đây là câu trả lời:\n```json\n" + BODY + "\n```\nHết.";
        assertThat(AnswerGenerator.parseJson(JSON, s).path("status").asText()).isEqualTo("answered");
    }

    @Test
    void bocThuaNgoacNhon() throws Exception {
        // lỗi gặp thật: "{{ ... }}" làm Jackson báo "Unexpected character '{' ... column 2"
        assertThat(AnswerGenerator.parseJson(JSON, "{" + BODY + "}").path("answer").asText())
                .isEqualTo("cao hơn {{K1}} [C40]");
    }

    @Test
    void chuTruocCoNgoacNhon() throws Exception {
        String s = "Kết quả {{K1}} sẽ được điền sau.\n" + BODY;
        assertThat(AnswerGenerator.parseJson(JSON, s).path("status").asText()).isEqualTo("answered");
    }

    @Test
    void khongCoJsonThiBaoLoi() {
        assertThatThrownBy(() -> AnswerGenerator.parseJson(JSON, "không có gì")).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> AnswerGenerator.parseJson(JSON, "{\"answer\": \"cụt")).isInstanceOf(IOException.class);
    }
}
