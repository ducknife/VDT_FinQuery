package com.vdt.finq.modules.conversation;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

// hội thoại, lượt hỏi đáp và feedback (V11); mọi truy vấn đều lọc theo nguoi_dung
@Repository
@RequiredArgsConstructor
public class ConversationRepository {

    public record ConversationRow(long id, String nguoiDung, String tieuDe, String tomTat, int soLuotTomTat,
            LocalDateTime capNhatLuc) {
    }

    public record MessageRow(long id, long conversationId, String cauHoi, String traLoi, String trangThai,
            String ketQuaJson, LocalDateTime taoLuc) {
    }

    public record FeedbackRow(long messageId, String muc, String lyDo, String cauHoi, String embedding) {
    }

    private final JdbcTemplate jdbc;

    public long createConversation(String user, String title) {
        return insert("INSERT INTO conversations (nguoi_dung, tieu_de) VALUES (?, ?)", user, title);
    }

    public long addMessage(long conversationId, String question, String answer, String status, String json) {
        long id = insert("INSERT INTO messages (conversation_id, cau_hoi, tra_loi, trang_thai, ket_qua_json) "
                + "VALUES (?, ?, ?, ?, ?)", conversationId, question, answer, status, json);
        jdbc.update("UPDATE conversations SET cap_nhat_luc = CURRENT_TIMESTAMP WHERE id = ?", conversationId);
        return id;
    }

    public Optional<ConversationRow> findConversation(long id, String user) {
        return jdbc.query("SELECT * FROM conversations WHERE id = ? AND nguoi_dung = ?", (rs, i) -> conversation(rs), id, user)
                .stream().findFirst();
    }

    public List<ConversationRow> listConversations(String user, int limit) {
        return jdbc.query("SELECT * FROM conversations WHERE nguoi_dung = ? ORDER BY cap_nhat_luc DESC LIMIT ?",
                (rs, i) -> conversation(rs), user, limit);
    }

    // chỉ chủ hội thoại; xoá kéo theo tin nhắn và feedback (ON DELETE CASCADE)
    public boolean deleteConversation(long id, String user) {
        return jdbc.update("DELETE FROM conversations WHERE id = ? AND nguoi_dung = ?", id, user) > 0;
    }

    public boolean renameConversation(long id, String user, String title) {
        return jdbc.update("UPDATE conversations SET tieu_de = ? WHERE id = ? AND nguoi_dung = ?", title, id, user) > 0;
    }

    public List<MessageRow> messages(long conversationId) {
        return jdbc.query("SELECT * FROM messages WHERE conversation_id = ? ORDER BY id", (rs, i) -> new MessageRow(
                rs.getLong("id"), rs.getLong("conversation_id"), rs.getString("cau_hoi"), rs.getString("tra_loi"),
                rs.getString("trang_thai"), rs.getString("ket_qua_json"), rs.getTimestamp("tao_luc").toLocalDateTime()),
                conversationId);
    }

    public void updateSummary(long id, String summary, int summarizedTurns) {
        jdbc.update("UPDATE conversations SET tom_tat = ?, so_luot_tom_tat = ? WHERE id = ?", summary, summarizedTurns, id);
    }

    // câu hỏi của một lượt, chỉ khi lượt đó thuộc hội thoại của người dùng
    public Optional<String> questionOfOwnMessage(long messageId, String user) {
        return jdbc.queryForList("SELECT m.cau_hoi FROM messages m JOIN conversations c ON c.id = m.conversation_id "
                + "WHERE m.id = ? AND c.nguoi_dung = ?", String.class, messageId, user).stream().findFirst();
    }

    public void upsertFeedback(long messageId, String user, String muc, String lyDo, String question, String embedding) {
        jdbc.update("INSERT INTO feedback (message_id, nguoi_dung, muc, ly_do, cau_hoi, embedding) VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE muc = VALUES(muc), ly_do = VALUES(ly_do), embedding = VALUES(embedding), "
                + "tao_luc = CURRENT_TIMESTAMP", messageId, user, muc, lyDo, question, embedding);
    }

    // feedback "sai" (một phần hoặc toàn bộ) của mọi người dùng: bài học cho câu tương tự
    public List<FeedbackRow> negativeFeedback() {
        return jdbc.query("SELECT message_id, muc, ly_do, cau_hoi, embedding FROM feedback "
                + "WHERE muc <> 'dung' AND embedding IS NOT NULL", (rs, i) -> new FeedbackRow(rs.getLong("message_id"),
                        rs.getString("muc"), rs.getString("ly_do"), rs.getString("cau_hoi"), rs.getString("embedding")));
    }

    public Optional<String> feedbackOf(long messageId, String user) {
        return jdbc.queryForList("SELECT muc FROM feedback WHERE message_id = ? AND nguoi_dung = ?", String.class,
                messageId, user).stream().findFirst();
    }

    private static ConversationRow conversation(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ConversationRow(rs.getLong("id"), rs.getString("nguoi_dung"), rs.getString("tieu_de"),
                rs.getString("tom_tat"), rs.getInt("so_luot_tom_tat"), rs.getTimestamp("cap_nhat_luc").toLocalDateTime());
    }

    private long insert(String sql, Object... args) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }
}
