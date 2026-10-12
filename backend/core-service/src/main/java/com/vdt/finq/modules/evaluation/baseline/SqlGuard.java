package com.vdt.finq.modules.evaluation.baseline;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// kiểm SQL do LLM viết (đối chứng text-to-SQL): chỉ cho MỘT câu SELECT/WITH, sai là bỏ, không sửa.
// lớp chặn thật vẫn là user chỉ đọc finq_reader
public final class SqlGuard {

    private static final int MAX_LENGTH = 4000;
    private static final Pattern STRING_LITERAL = Pattern.compile("'(?:[^'\\\\]|\\\\.|'')*'|\"(?:[^\"\\\\]|\\\\.)*\"");
    private static final Pattern FORBIDDEN = Pattern.compile("\\b(INSERT|UPDATE|DELETE|REPLACE|MERGE|DROP|ALTER|CREATE"
            + "|TRUNCATE|RENAME|GRANT|REVOKE|SET|CALL|DO|HANDLER|LOAD|LOCK|UNLOCK|INTO|OUTFILE|DUMPFILE|SLEEP|BENCHMARK"
            + "|GET_LOCK|SHUTDOWN|KILL|EXECUTE|PREPARE|DEALLOCATE)\\b");

    private SqlGuard() {
    }

    // null nếu hợp lệ, ngược lại là lý do từ chối (gửi lại cho LLM)
    public static String reject(String sql) {
        if (sql == null || sql.isBlank()) {
            return "empty query";
        }
        String s = sql.strip();
        if (s.length() > MAX_LENGTH) {
            return "query too long";
        }
        if (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).strip();
        }
        // bỏ chuỗi trong nháy trước khi soát từ khoá, để 'SET' trong dữ liệu không bị chặn nhầm
        String code = STRING_LITERAL.matcher(s).replaceAll("''");
        if (code.contains(";")) {
            return "only a single statement is allowed";
        }
        if (code.contains("--") || code.contains("/*") || code.contains("#")) {
            return "comments are not allowed";
        }
        String upper = code.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            return "only SELECT (or WITH ... SELECT) is allowed";
        }
        Matcher m = FORBIDDEN.matcher(upper);
        if (m.find()) {
            return "forbidden keyword " + m.group(1) + ": only read-only SELECT is allowed";
        }
        return null;
    }
}
