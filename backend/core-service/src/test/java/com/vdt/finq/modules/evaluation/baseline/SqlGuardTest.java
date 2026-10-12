package com.vdt.finq.modules.evaluation.baseline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SqlGuardTest {

    @Test
    void choPhepSelect() {
        assertThat(SqlGuard.reject("SELECT gia_tri FROM financial_figures WHERE ma_noi_bo = 'DOANH_THU_THUAN'")).isNull();
        assertThat(SqlGuard.reject("select count(*) from financial_items;")).isNull();
        assertThat(SqlGuard.reject("WITH a AS (SELECT 1 AS x) SELECT x FROM a")).isNull();
        // từ khoá nằm trong chuỗi dữ liệu thì không bị chặn
        assertThat(SqlGuard.reject("SELECT * FROM documents WHERE ten_hien_thi = 'SET UPDATE'")).isNull();
    }

    @Test
    void chanGhiXoa() {
        assertThat(SqlGuard.reject("DELETE FROM financial_figures")).isNotNull();
        assertThat(SqlGuard.reject("UPDATE documents SET ten_hien_thi = 'x'")).isNotNull();
        assertThat(SqlGuard.reject("DROP TABLE financial_figures")).isNotNull();
        assertThat(SqlGuard.reject("SELECT 1; DROP TABLE financial_figures")).contains("single statement");
        assertThat(SqlGuard.reject("SELECT * INTO OUTFILE '/tmp/x' FROM financial_figures")).isNotNull();
        assertThat(SqlGuard.reject("SELECT SLEEP(10)")).isNotNull();
        assertThat(SqlGuard.reject("SELECT 1 -- DROP")).contains("comments");
        assertThat(SqlGuard.reject("")).isNotNull();
    }
}
