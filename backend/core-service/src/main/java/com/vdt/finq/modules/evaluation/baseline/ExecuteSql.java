package com.vdt.finq.modules.evaluation.baseline;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.vdt.finq.common.db.ReadOnlyJdbc;
import com.vdt.finq.modules.agent.evidence.Evidence;
import com.vdt.finq.modules.agent.tools.QueryState;
import com.vdt.finq.modules.dbquery.support.VnNumber;

// tool của đối chứng text-to-SQL (không đăng ký cho agent FinQuery): chạy câu SELECT do LLM viết
// bằng user chỉ đọc; kết quả thành bằng chứng D# để đi qua cùng bước trả lời và kiểm như FinQuery
@Component
public class ExecuteSql {

    private static final int MAX_ROWS = 200;
    private static final int TIMEOUT_SECONDS = 5;

    private final ReadOnlyJdbc readOnlyJdbc;

    public ExecuteSql(ReadOnlyJdbc readOnlyJdbc) {
        this.readOnlyJdbc = readOnlyJdbc;
    }

    @Tool(name = "execute_sql", description = """
            Run ONE read-only MySQL SELECT statement on the financial database and return the rows (at most 200).
            Only SELECT or WITH ... SELECT is accepted; anything else is rejected. Tables are described in the
            system prompt.""")
    public String executeSql(@ToolParam(description = "A single MySQL SELECT statement") String query,
            ToolContext toolContext) {
        QueryState state = QueryState.of(toolContext);
        long start = System.nanoTime();
        if (!readOnlyJdbc.isDedicatedUser()) {
            return "Error: the read-only database user is not configured; SQL execution is disabled.";
        }
        String reason = SqlGuard.reject(query);
        if (reason != null) {
            state.record("execute_sql BỊ CHẶN (" + reason + "): " + oneLine(query), elapsed(start), 0);
            return "Rejected: " + reason + ".";
        }
        JdbcTemplate jdbc = new JdbcTemplate(readOnlyJdbc.template().getDataSource());
        jdbc.setMaxRows(MAX_ROWS + 1);
        jdbc.setQueryTimeout(TIMEOUT_SECONDS);
        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList(query);
        } catch (DataAccessException e) {
            String msg = e.getMostSpecificCause().getMessage();
            state.record("execute_sql LỖI (" + msg + "): " + oneLine(query), elapsed(start), 0);
            return "SQL error: " + msg;
        }
        String table = table(rows);
        Evidence e = state.getEvidence().addComputed("Kết quả SQL (" + Math.min(rows.size(), MAX_ROWS) + " dòng"
                + (rows.size() > MAX_ROWS ? ", đã cắt" : "") + "):\n" + oneLine(query) + "\n" + table);
        state.record("execute_sql -> " + rows.size() + " dòng: " + oneLine(query), elapsed(start), 0);
        return "[" + e.getId() + "] " + e.getContent();
    }

    // bảng markdown; số nguyên viết kiểu Việt Nam để bước trả lời và bộ kiểm số dùng chung cách viết
    private static String table(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "(no rows)";
        }
        List<String> cols = List.copyOf(rows.get(0).keySet());
        StringBuilder s = new StringBuilder("| ").append(String.join(" | ", cols)).append(" |\n|")
                .append("---|".repeat(cols.size())).append('\n');
        for (Map<String, Object> r : rows.subList(0, Math.min(rows.size(), MAX_ROWS))) {
            s.append("| ");
            for (String c : cols) {
                s.append(cell(r.get(c))).append(" | ");
            }
            s.append('\n');
        }
        return s.toString();
    }

    private static String cell(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Long || v instanceof Integer || v instanceof BigInteger) {
            return VnNumber.dong(((Number) v).longValue());
        }
        if (v instanceof BigDecimal d) {
            return d.scale() <= 0 ? VnNumber.dong(d.longValue()) : d.stripTrailingZeros().toPlainString().replace('.', ',');
        }
        if (v instanceof Double || v instanceof Float) {
            return BigDecimal.valueOf(((Number) v).doubleValue()).stripTrailingZeros().toPlainString().replace('.', ',');
        }
        return v.toString().replace('|', '/');
    }

    private static String oneLine(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").strip();
    }

    private static long elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
