-- Hội thoại, lượt hỏi đáp và feedback (QĐ-038: người dùng ở Keycloak, đây chỉ lưu mã sub).

CREATE TABLE conversations (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nguoi_dung    VARCHAR(64)     NOT NULL COMMENT 'sub trong token Keycloak',
    tieu_de       VARCHAR(200)    NOT NULL COMMENT 'câu hỏi đầu tiên, cắt ngắn',
    tom_tat       TEXT                NULL COMMENT 'tóm tắt các lượt cũ (compaction), giữ số, kỳ, nguồn',
    so_luot_tom_tat INT UNSIGNED  NOT NULL DEFAULT 0 COMMENT 'số lượt đầu đã gộp vào tom_tat',
    tao_luc       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cap_nhat_luc  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_conversations_nguoi_dung (nguoi_dung, cap_nhat_luc)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE messages (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT UNSIGNED NOT NULL,
    cau_hoi         TEXT            NOT NULL,
    tra_loi         MEDIUMTEXT          NULL,
    trang_thai      VARCHAR(20)         NULL COMMENT 'answered | partial | not_found | out_of_scope',
    ket_qua_json    MEDIUMTEXT          NULL COMMENT 'AskResult đầy đủ: trích dẫn, thời gian, token, chi phí',
    tao_luc         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- mỗi người một feedback cho một câu trả lời; sai thì bắt buộc có lý do
CREATE TABLE feedback (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    message_id  BIGINT UNSIGNED NOT NULL,
    nguoi_dung  VARCHAR(64)     NOT NULL,
    muc         VARCHAR(16)     NOT NULL COMMENT 'dung | sai_mot_phan | sai_toan_bo',
    ly_do       VARCHAR(1000)       NULL,
    cau_hoi     TEXT            NOT NULL COMMENT 'chép từ messages để tra câu tương tự',
    embedding   MEDIUMTEXT          NULL COMMENT 'vector của câu hỏi (JSON), để tìm câu tương tự',
    tao_luc     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_feedback_message_nguoi_dung (message_id, nguoi_dung),
    CONSTRAINT fk_feedback_message FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT ck_feedback_muc CHECK (muc IN ('dung', 'sai_mot_phan', 'sai_toan_bo')),
    CONSTRAINT ck_feedback_ly_do CHECK (muc = 'dung' OR (ly_do IS NOT NULL AND CHAR_LENGTH(TRIM(ly_do)) > 0))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
