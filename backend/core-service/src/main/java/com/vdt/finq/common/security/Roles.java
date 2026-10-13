package com.vdt.finq.common.security;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// vai trò realm Keycloak (QĐ-043), lồng nhau ADMIN ⊃ DOCUMENT_MANAGER ⊃ ANALYST; token mang vai trò đã mở rộng.
// quyền theo vai trò ở đây là nguồn duy nhất cho SecurityConfig và /api/v1/me
public final class Roles {

    // hỏi đáp, xem trích dẫn và trang tài liệu, lịch sử hội thoại, feedback
    public static final String ANALYST = "analyst";
    // + nạp, index, trích số tài liệu; thử truy hồi
    public static final String DOCUMENT_MANAGER = "document_manager";
    // + API thử /test/**; duyệt, quản lý tài khoản thì làm ở trang quản trị realm finquery của Keycloak
    public static final String ADMIN = "admin";

    public static final Set<String> ALL = Set.of(ANALYST, DOCUMENT_MANAGER, ADMIN);
    public static final String[] ANY = { ANALYST, DOCUMENT_MANAGER, ADMIN };
    public static final String[] DOCUMENTS = { DOCUMENT_MANAGER, ADMIN };

    private Roles() {
    }

    // việc người dùng được làm, cho giao diện ẩn/hiện chức năng (core vẫn kiểm lại ở SecurityConfig)
    public static Map<String, Boolean> permissions(Collection<String> roles) {
        Map<String, Boolean> p = new LinkedHashMap<>();
        p.put("ask", roles.stream().anyMatch(ALL::contains));
        p.put("manageDocuments", roles.contains(DOCUMENT_MANAGER) || roles.contains(ADMIN));
        p.put("manageUsers", roles.contains(ADMIN));
        return p;
    }
}
