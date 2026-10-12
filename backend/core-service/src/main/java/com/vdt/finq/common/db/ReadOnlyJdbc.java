package com.vdt.finq.common.db;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

// kết nối chỉ đọc cho mọi SQL do agent kích hoạt: user DB_READONLY_* (chỉ SELECT 3 bảng) nếu đã cấu hình,
// không thì kết nối chính (chỉ an toàn khi gọi trong giao dịch READ ONLY)
@Slf4j
@Component
public class ReadOnlyJdbc {

    private final JdbcTemplate jdbc;
    private final HikariDataSource ownPool;

    public ReadOnlyJdbc(JdbcTemplate primary, @Value("${spring.datasource.url}") String url,
            @Value("${financial-query.readonly-username:}") String user,
            @Value("${financial-query.readonly-password:}") String password) {
        if (user == null || user.isBlank()) {
            log.warn("chưa cấu hình DB_READONLY_USERNAME: tool số liệu dùng kết nối chính trong giao dịch READ ONLY");
            this.jdbc = primary;
            this.ownPool = null;
            return;
        }
        // pool riêng, không đăng ký làm bean để không đè DataSource chính
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setReadOnly(true);
        ds.setMaximumPoolSize(4);
        ds.setPoolName("finq-readonly");
        this.ownPool = ds;
        this.jdbc = new JdbcTemplate((DataSource) ds);
        log.info("tool số liệu dùng user chỉ đọc {}", user);
    }

    public JdbcTemplate template() {
        return jdbc;
    }

    // true khi đang dùng user chỉ đọc riêng (MySQL chặn mọi lệnh ghi)
    public boolean isDedicatedUser() {
        return ownPool != null;
    }

    @PreDestroy
    void close() {
        if (ownPool != null) {
            ownPool.close();
        }
    }
}
