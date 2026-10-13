package com.vdt.finq.modules.account;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.finq.common.security.CurrentUser;

import lombok.RequiredArgsConstructor;

// /api/v1/admin/users: chỉ admin (SecurityConfig); /api/v1/auth/forgot-password: ai cũng gọi được
@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    public record CreateUser(String email, String firstName, String lastName, String role) {
    }

    public record RoleChange(String role) {
    }

    public record EnabledChange(boolean enabled) {
    }

    public record ForgotPassword(String email) {
    }

    public record Profile(String lastName, String firstName) {
    }

    public record PasswordChange(String currentPassword, String newPassword) {
    }

    // người dùng tự sửa thông tin của mình
    @PutMapping("/api/v1/me/profile")
    public Map<String, Object> profile(@RequestBody Profile body) {
        accountService.updateProfile(CurrentUser.get(), body.lastName(), body.firstName());
        return Map.of("status", "updated");
    }

    @PutMapping("/api/v1/me/password")
    public Map<String, Object> password(@RequestBody PasswordChange body) {
        accountService.changePassword(CurrentUser.get(), body.currentPassword(), body.newPassword());
        return Map.of("status", "updated");
    }

    @GetMapping("/api/v1/admin/users")
    public List<Map<String, Object>> list(@RequestParam(required = false) String search) {
        return accountService.list(search);
    }

    @PostMapping("/api/v1/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody CreateUser body) {
        return accountService.create(body.email(), body.firstName(), body.lastName(), body.role());
    }

    @PutMapping("/api/v1/admin/users/{id}/role")
    public Map<String, Object> role(@PathVariable String id, @RequestBody RoleChange body) {
        accountService.setRole(CurrentUser.get(), id, body.role());
        return Map.of("id", id, "role", body.role());
    }

    @PutMapping("/api/v1/admin/users/{id}/enabled")
    public Map<String, Object> enabled(@PathVariable String id, @RequestBody EnabledChange body) {
        accountService.setEnabled(CurrentUser.get(), id, body.enabled());
        return Map.of("id", id, "enabled", body.enabled());
    }

    @PostMapping("/api/v1/admin/users/{id}/reset-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> resetPassword(@PathVariable String id) {
        accountService.sendPasswordReset(id);
        return Map.of("id", id, "status", "sent");
    }

    @PostMapping("/api/v1/auth/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> forgotPassword(@RequestBody ForgotPassword body) {
        accountService.forgotPassword(body == null ? null : body.email());
        return Map.of("message", "Nếu email có tài khoản FinQuery, hướng dẫn đặt lại mật khẩu đã được gửi tới hộp thư.");
    }
}
