package com.vdt.finq.modules.rag.generation;

// đọc dần JSON đang stream {"status": ..., "answer": "...", ...} và trả phần chữ MỚI của trường "answer"
// (đã giải mã \n, \", \\, \\uXXXX) ngay khi model sinh ra, để giao diện hiện chữ chạy dần
public final class AnswerFieldStreamer {

    private enum State {
        SEEK, IN_VALUE, DONE
    }

    private static final String KEY = "\"answer\"";

    private final StringBuilder raw = new StringBuilder();
    private State state = State.SEEK;
    private int pos;

    // nạp thêm một mẩu, trả phần chữ của answer giải mã được thêm (có thể rỗng)
    public String feed(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }
        // luôn giữ toàn bộ JSON (citations, missing sau answer) để đọc sau khi stream xong
        raw.append(chunk);
        if (state == State.DONE) {
            return "";
        }
        if (state == State.SEEK && !seekValueStart()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        while (pos < raw.length()) {
            char c = raw.charAt(pos);
            if (c == '"') {
                state = State.DONE;
                pos++;
                break;
            }
            if (c != '\\') {
                out.append(c);
                pos++;
                continue;
            }
            // escape: chờ đủ ký tự mới giải mã
            if (pos + 1 >= raw.length()) {
                break;
            }
            char e = raw.charAt(pos + 1);
            if (e == 'u') {
                if (pos + 6 > raw.length()) {
                    break;
                }
                out.append((char) Integer.parseInt(raw.substring(pos + 2, pos + 6), 16));
                pos += 6;
                continue;
            }
            out.append(switch (e) {
                case 'n' -> '\n';
                case 't' -> '\t';
                case 'r' -> '\r';
                case 'b' -> '\b';
                case 'f' -> '\f';
                default -> e;
            });
            pos += 2;
        }
        return out.toString();
    }

    // tìm "answer" : " rồi đặt pos ngay sau dấu nháy mở
    private boolean seekValueStart() {
        int k = raw.indexOf(KEY);
        if (k < 0) {
            return false;
        }
        int i = k + KEY.length();
        while (i < raw.length() && Character.isWhitespace(raw.charAt(i))) {
            i++;
        }
        if (i >= raw.length() || raw.charAt(i) != ':') {
            return false;
        }
        i++;
        while (i < raw.length() && Character.isWhitespace(raw.charAt(i))) {
            i++;
        }
        if (i >= raw.length() || raw.charAt(i) != '"') {
            return false;
        }
        pos = i + 1;
        state = State.IN_VALUE;
        return true;
    }

    public String raw() {
        return raw.toString();
    }
}
