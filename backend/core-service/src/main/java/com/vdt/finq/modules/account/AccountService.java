package com.vdt.finq.modules.account;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.exception.InvalidRequestException;
import com.vdt.finq.common.security.CurrentUser;
import com.vdt.finq.common.security.Roles;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// quản lý tài khoản FinQuery qua Keycloak (QĐ-046): mỗi người một vai trò FinQuery gán trực tiếp
// (admin ⊃ document_manager ⊃ analyst; "pending" = chưa có vai trò, chờ duyệt); mật khẩu do người dùng tự đặt từ link trong mail
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    public static final String PENDING = "pending";
    private static final List<String> ORDER = List.of(Roles.ADMIN, Roles.DOCUMENT_MANAGER, Roles.ANALYST);
    // quên mật khẩu: mỗi email tối đa 1 lần / 60 giây
    private static final long RESET_COOLDOWN_SECONDS = 60;

    private final KeycloakAdminClient keycloak;
    private final Map<String, Instant> lastReset = new ConcurrentHashMap<>();

    public List<Map<String, Object>> list(String search) {
        return keycloak.users(search, 0, 200).stream()
                .filter(u -> !String.valueOf(u.get("username")).startsWith("service-account-"))
                .map(this::view).toList();
    }

    // tạo tài khoản, gán vai trò, gửi mail để người dùng tự đặt mật khẩu
    public Map<String, Object> create(String email, String firstName, String lastName, String role) {
        String e = email == null ? "" : email.strip().toLowerCase();
        if (e.isEmpty() || isBlank(firstName) || isBlank(lastName)) {
            throw new InvalidRequestException("cần email, họ và tên");
        }
        checkRole(role);
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("username", e);
        user.put("email", e);
        user.put("firstName", firstName.strip());
        user.put("lastName", lastName.strip());
        user.put("enabled", true);
        // quản trị viên tạo từ danh bạ công ty nên coi email đã xác minh
        user.put("emailVerified", true);
        String id = keycloak.createUser(user);
        setRoleOf(id, role);
        keycloak.sendActionsEmail(id, List.of("UPDATE_PASSWORD"));
        log.info("tạo tài khoản {} vai trò {}, đã gửi mail đặt mật khẩu", e, role);
        return Map.of("id", id, "email", e, "role", role);
    }

    public void setRole(CurrentUser actor, String id, String role) {
        checkRole(role);
        if (id.equals(actor.id()) && !Roles.ADMIN.equals(role)) {
            throw new InvalidRequestException("không tự hạ quyền quản trị của chính mình");
        }
        setRoleOf(id, role);
    }

    public void setEnabled(CurrentUser actor, String id, boolean enabled) {
        if (id.equals(actor.id()) && !enabled) {
            throw new InvalidRequestException("không tự khoá tài khoản của chính mình");
        }
        keycloak.updateUser(id, Map.of("enabled", enabled));
    }

    public void sendPasswordReset(String id) {
        keycloak.sendActionsEmail(id, List.of("UPDATE_PASSWORD"));
    }

    // người dùng tự sửa họ tên (ghi vào Keycloak; token mới mới có tên mới)
    public void updateProfile(CurrentUser user, String lastName, String firstName) {
        requireAccount(user);
        if (isBlank(lastName) || isBlank(firstName)) {
            throw new InvalidRequestException("cần họ và tên");
        }
        keycloak.updateUser(user.id(), Map.of("lastName", lastName.strip(), "firstName", firstName.strip()));
    }

    // người dùng tự đổi mật khẩu: phải đúng mật khẩu hiện tại; Keycloak kiểm chính sách mật khẩu mới
    public void changePassword(CurrentUser user, String current, String next) {
        requireAccount(user);
        if (isBlank(current) || isBlank(next)) {
            throw new InvalidRequestException("cần mật khẩu hiện tại và mật khẩu mới");
        }
        if (!keycloak.passwordMatches(user.name(), current)) {
            throw new InvalidRequestException("Mật khẩu hiện tại không đúng");
        }
        try {
            keycloak.resetPassword(user.id(), next);
        } catch (AppException e) {
            throw new InvalidRequestException(policyMessage(e.getMessage()));
        }
    }

    // chế độ không bảo mật (dev) không có tài khoản Keycloak thật
    private static void requireAccount(CurrentUser user) {
        if (CurrentUser.DEV.id().equals(user.id())) {
            throw new InvalidRequestException("chế độ dev (tắt bảo mật) không có tài khoản Keycloak để sửa");
        }
    }

    // lỗi chính sách mật khẩu của Keycloak Admin API (tiếng Anh) thành câu tiếng Việt
    static String policyMessage(String raw) {
        String m = raw == null ? "" : raw.toLowerCase();
        if (m.contains("minimum length")) return "Mật khẩu mới phải có ít nhất 12 ký tự.";
        if (m.contains("maximum length")) return "Mật khẩu mới dài tối đa 64 ký tự.";
        if (m.contains("upper case")) return "Mật khẩu mới phải có chữ hoa.";
        if (m.contains("lower case")) return "Mật khẩu mới phải có chữ thường.";
        if (m.contains("digit")) return "Mật khẩu mới phải có chữ số.";
        if (m.contains("special")) return "Mật khẩu mới phải có ký tự đặc biệt.";
        if (m.contains("regex")) return "Mật khẩu mới không được chứa dấu nháy hoặc tên công ty, hệ thống (Viettel, VCC, CTR, FinQuery).";
        if (m.contains("username")) return "Mật khẩu mới không được chứa tên đăng nhập.";
        if (m.contains("email")) return "Mật khẩu mới không được trùng email.";
        if (m.contains("recently used") || m.contains("history")) return "Không được dùng lại 5 mật khẩu gần nhất.";
        return raw;
    }

    // quên mật khẩu: luôn trả cùng một câu, không cho biết email có tồn tại hay không
    public void forgotPassword(String email) {
        String e = email == null ? "" : email.strip().toLowerCase();
        if (e.isEmpty() || !keycloak.isConfigured()) {
            if (!keycloak.isConfigured()) {
                log.warn("quên mật khẩu: chưa cấu hình KEYCLOAK_CORE_CLIENT_SECRET, không gửi được mail");
            }
            return;
        }
        Instant last = lastReset.get(e);
        if (last != null && last.plusSeconds(RESET_COOLDOWN_SECONDS).isAfter(Instant.now())) {
            return;
        }
        lastReset.put(e, Instant.now());
        try {
            keycloak.findByEmail(e).filter(u -> Boolean.TRUE.equals(u.get("enabled")))
                    .ifPresent(u -> keycloak.sendActionsEmail(String.valueOf(u.get("id")), List.of("UPDATE_PASSWORD")));
        } catch (AppException ex) {
            log.warn("quên mật khẩu: không gửi được mail: {}", ex.getMessage());
        }
    }

    // bỏ mọi vai trò FinQuery đang gán trực tiếp rồi gán vai trò mới (PENDING = không gán)
    private void setRoleOf(String id, String role) {
        List<Map<String, Object>> current = keycloak.realmRoles(id).stream()
                .filter(r -> Roles.ALL.contains(String.valueOf(r.get("name")))).toList();
        if (!current.isEmpty()) {
            keycloak.removeRealmRoles(id, current);
        }
        if (!PENDING.equals(role)) {
            keycloak.addRealmRoles(id, List.of(keycloak.role(role)));
        }
    }

    private Map<String, Object> view(Map<String, Object> u) {
        String id = String.valueOf(u.get("id"));
        List<String> names = keycloak.realmRoles(id).stream().map(r -> String.valueOf(r.get("name"))).toList();
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", id);
        v.put("email", u.getOrDefault("email", u.get("username")));
        v.put("firstName", u.getOrDefault("firstName", ""));
        v.put("lastName", u.getOrDefault("lastName", ""));
        v.put("enabled", u.get("enabled"));
        v.put("emailVerified", u.get("emailVerified"));
        v.put("createdAt", u.get("createdTimestamp"));
        v.put("role", ORDER.stream().filter(names::contains).findFirst().orElse(PENDING));
        return v;
    }

    private static void checkRole(String role) {
        if (!Roles.ALL.contains(role) && !PENDING.equals(role)) {
            throw new InvalidRequestException("vai trò phải là analyst, document_manager, admin hoặc pending");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
