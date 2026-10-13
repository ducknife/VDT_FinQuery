package com.vdt.finq.modules.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.security.CurrentUser;

// chạy thật với Keycloak + Mailpit đang chạy (cần biến môi trường KEYCLOAK_CORE_CLIENT_SECRET):
//     ./mvnw test "-Dtest=AccountLiveTest" "-Daccount.live=true"
// tạo một tài khoản tạm rồi xoá khi xong
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "answer.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "account.live", matches = "true")
class AccountLiveTest {

    private static final String EMAIL = "kiem.tra.tai.khoan@viettel.com.vn";

    @Autowired
    private AccountService accounts;
    @Autowired
    private KeycloakAdminClient keycloak;

    private Map<String, Object> find() {
        return accounts.list(EMAIL).stream().filter(u -> EMAIL.equals(u.get("email"))).findFirst().orElseThrow();
    }

    private static String mailpit(String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:8025" + path)).build(),
                HttpResponse.BodyHandlers.ofString()).body();
    }

    @Test
    void tuSuaHoTenVaDoiMatKhau() {
        keycloak.findByEmail(EMAIL).ifPresent(u -> keycloak.deleteUser(String.valueOf(u.get("id"))));
        String id = String.valueOf(accounts.create(EMAIL, "Kiểm", "Tra", "analyst").get("id"));
        CurrentUser self = new CurrentUser(id, EMAIL, EMAIL, java.util.List.of("analyst"));
        try {
            keycloak.resetPassword(id, "Mat-Khau#Cu2026");
            accounts.updateProfile(self, "Nguyễn", "Văn Thử");
            assertThat(find()).containsEntry("lastName", "Nguyễn").containsEntry("firstName", "Văn Thử");

            assertThatThrownBy(() -> accounts.changePassword(self, "sai-mat-khau", "Mat-Khau#Moi2026"))
                    .hasMessageContaining("hiện tại không đúng");
            assertThatThrownBy(() -> accounts.changePassword(self, "Mat-Khau#Cu2026", "Ngan#1"))
                    .hasMessageContaining("ít nhất 12");
            assertThatThrownBy(() -> accounts.changePassword(self, "Mat-Khau#Cu2026", "Viettel-Mat#Khau2026"))
                    .satisfies(e -> System.out.println("[ACC] chứa Viettel: " + e.getMessage()));
            accounts.changePassword(self, "Mat-Khau#Cu2026", "Mat-Khau#Moi2026");
            assertThat(keycloak.passwordMatches(EMAIL, "Mat-Khau#Moi2026")).isTrue();
            assertThat(keycloak.passwordMatches(EMAIL, "Mat-Khau#Cu2026")).isFalse();
            System.out.println("[ACC] đổi mật khẩu: đúng");
        } finally {
            keycloak.deleteUser(id);
        }
    }

    @Test
    void vongDoiTaiKhoan() throws Exception {
        keycloak.findByEmail(EMAIL).ifPresent(u -> keycloak.deleteUser(String.valueOf(u.get("id"))));
        mailpit("/api/v1/messages").length();
        String id = String.valueOf(accounts.create(EMAIL, "Kiểm", "Tra", "analyst").get("id"));
        try {
            assertThat(find()).containsEntry("role", "analyst").containsEntry("enabled", true).containsEntry("firstName", "Kiểm");
            System.out.println("[ACC] tạo: " + find());

            accounts.setRole(CurrentUser.DEV, id, "document_manager");
            assertThat(find()).containsEntry("role", "document_manager");
            accounts.setRole(CurrentUser.DEV, id, AccountService.PENDING);
            assertThat(find()).containsEntry("role", "pending");
            accounts.setEnabled(CurrentUser.DEV, id, false);
            assertThat(find()).containsEntry("enabled", false);
            accounts.setEnabled(CurrentUser.DEV, id, true);

            // tự hạ quyền / tự khoá chính mình bị chặn
            assertThatThrownBy(() -> accounts.setRole(new CurrentUser(id, EMAIL, EMAIL, java.util.List.of()), id, "analyst"))
                    .isInstanceOf(AppException.class);

            accounts.sendPasswordReset(id);
            accounts.forgotPassword(EMAIL);
            Thread.sleep(1500);
            String inbox = mailpit("/api/v1/search?query=to:" + EMAIL);
            long mails = inbox.split("\"Subject\"").length - 1;
            System.out.println("[ACC] mail tới " + EMAIL + ": " + mails);
            assertThat(mails).isGreaterThanOrEqualTo(3);

            // email ngoài công ty bị Keycloak chặn (luật User Profile)
            assertThatThrownBy(() -> accounts.create("ai.do@gmail.com", "A", "B", "analyst")).isInstanceOf(AppException.class)
                    .satisfies(e -> System.out.println("[ACC] gmail: " + e.getMessage()));
        } finally {
            keycloak.deleteUser(id);
            System.out.println("[ACC] đã xoá tài khoản tạm");
        }
    }
}
