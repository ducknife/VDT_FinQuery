package com.vdt.finq.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class TableToJsonTest {

    private final TableToJson tableToJson = new TableToJson(new ObjectMapper());

    // mỗi dòng bảng thành một object, số giữ nguyên chuỗi gốc và đi kèm tên cột
    @Test
    void convertsTableKeepingTextAround() {
        String chunk = """
                ## BẢNG CÂN ĐỐI KẾ TOÁN HỢP NHẤT
                | Mã số | TÀI SẢN | 31/12/2023 VND |
                |---|---|---|
                | 100 | A. TÀI SẢN NGẮN HẠN | 5.627.168.242.619 |
                | 137 | 4. Dự phòng phải thu ngắn hạn khó đòi | (103.552.387.501) |
                Ghi chú cuối trang""";
        String json = tableToJson.convert(chunk);
        assertThat(json).startsWith("## BẢNG CÂN ĐỐI KẾ TOÁN HỢP NHẤT\n[");
        assertThat(json).contains("{\"Mã số\":\"100\",\"TÀI SẢN\":\"A. TÀI SẢN NGẮN HẠN\",\"31/12/2023 VND\":\"5.627.168.242.619\"}");
        assertThat(json).contains("\"31/12/2023 VND\":\"(103.552.387.501)\"");
        assertThat(json).endsWith("]\nGhi chú cuối trang");
    }

    // tiêu đề hai tầng: ô gộp "Số liệu lũy kế" phủ hai cột bên dưới
    @Test
    void mergesMultiLevelHeaders() {
        String chunk = """
                | Mã số | Chỉ tiêu | Số liệu phát sinh trong kỳ | | Số liệu lũy kế | |
                |---|---|---|---|---|---|
                | | | Quý 4/2019 | Quý 4/2018 | Quý 4/2019 | Quý 4/2018 |
                | 01 | Doanh thu | 1.371.435.266.879 | 1.169.805.727.596 | 5.046.043.734.972 | 4.276.731.191.297 |""";
        String json = tableToJson.convert(chunk);
        assertThat(json).contains("\"Số liệu lũy kế - Quý 4/2019\":\"5.046.043.734.972\"");
        assertThat(json).contains("\"Số liệu phát sinh trong kỳ - Quý 4/2018\":\"1.169.805.727.596\"");
    }

    @Test
    void leavesTextWithoutTablesUntouched() {
        String text = "Doanh thu tăng 12% so với cùng kỳ.";
        assertThat(tableToJson.convert(text)).isEqualTo(text);
    }
}
