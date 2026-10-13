package com.vdt.finq.common.security;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

// người đang gọi API: sub, tên đăng nhập và vai trò FinQuery trong token Keycloak; tắt bảo mật thì là "dev" đủ quyền.
// phải đọc trên luồng của request (luồng nền không có SecurityContext)
// displayName: họ tên kiểu Việt Nam (họ trước, family_name + given_name), không có thì dùng tên đăng nhập
public record CurrentUser(String id, String name, String displayName, List<String> roles) {

    public static final CurrentUser DEV = new CurrentUser("dev", "dev", "Dev",
            List.of(Roles.ADMIN, Roles.DOCUMENT_MANAGER, Roles.ANALYST));

    public static CurrentUser get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a instanceof JwtAuthenticationToken jwt) {
            String name = jwt.getToken().getClaimAsString("preferred_username");
            List<String> roles = jwt.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                    .filter(r -> r.startsWith("ROLE_")).map(r -> r.substring(5)).filter(Roles.ALL::contains).sorted().toList();
            String login = name == null ? jwt.getToken().getSubject() : name;
            String full = String.join(" ", java.util.stream.Stream.of(jwt.getToken().getClaimAsString("family_name"),
                    jwt.getToken().getClaimAsString("given_name")).filter(x -> x != null && !x.isBlank()).toList());
            return new CurrentUser(jwt.getToken().getSubject(), login, full.isBlank() ? login : full, roles);
        }
        return DEV;
    }

    // việc được làm: ask, manageDocuments, manageUsers
    public Map<String, Boolean> permissions() {
        return Roles.permissions(roles);
    }
}
