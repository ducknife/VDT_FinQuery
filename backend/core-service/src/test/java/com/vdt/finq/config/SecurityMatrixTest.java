package com.vdt.finq.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.vdt.finq.common.security.Roles;

// bảng quyền của core khi bật bảo mật (QĐ-043), dùng token giả lập, không cần Keycloak, không gọi LLM/embedding:
// mỗi request chọn sao cho dừng trước phần tốn tiền (400/404 nghĩa là đã qua kiểm quyền)
@SpringBootTest(properties = { "finq.security.enabled=true", "finq.keycloak.client-secret=", "cleaner.trial.enabled=false", "answer.trial.enabled=false",
        "cleaner.test-api.enabled=true" })
@AutoConfigureMockMvc
class SecurityMatrixTest {

    @Autowired
    private MockMvc mvc;

    // token có realm_access.roles như Keycloak phát, đi qua đúng converter của SecurityConfig
    private static JwtRequestPostProcessor token(String... roles) {
        Jwt j = Jwt.withTokenValue("t").header("alg", "none").subject("sub-" + String.join("-", roles))
                .claim("preferred_username", "u@viettel.com.vn").claim("realm_access", Map.of("roles", List.of(roles)))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        var auth = SecurityConfig.keycloakRoles().convert(j);
        return jwt().jwt(j).authorities(auth.getAuthorities().toArray(GrantedAuthority[]::new));
    }

    private int code(MockHttpServletRequestBuilder request, JwtRequestPostProcessor token) throws Exception {
        RequestBuilder r = token == null ? request : request.with(token);
        return mvc.perform(r).andReturn().getResponse().getStatus();
    }

    @Test
    void bangQuyenTheoVaiTro() throws Exception {
        // tên | request | mã mong đợi: không token, chưa duyệt (không vai trò), analyst, document_manager, admin
        Object[][] matrix = {
                { "xem mình là ai", get("/api/v1/me"), new int[] { 401, 200, 200, 200, 200 } },
                { "lịch sử hội thoại", get("/api/v1/conversations"), new int[] { 401, 403, 200, 200, 200 } },
                { "gửi feedback (thiếu messageId)", post("/api/v1/feedback").contentType(MediaType.APPLICATION_JSON).content("{}"),
                        new int[] { 401, 403, 400, 400, 400 } },
                { "xem tài liệu (mã không có)", get("/api/v1/documents/KHONG-CO"), new int[] { 401, 403, 404, 404, 404 } },
                { "index tài liệu (mã không có)", post("/api/v1/documents/KHONG-CO/index"), new int[] { 401, 403, 403, 404, 404 } },
                { "thử truy hồi (câu trống)", get("/api/v1/retrieval/search").param("q", " "), new int[] { 401, 403, 403, 400, 400 } },
                { "API thử bộ làm sạch (thiếu path)", get("/test/cleaner/page"), new int[] { 401, 403, 403, 403, 400 } },
                { "danh sách tài liệu", get("/api/v1/documents").param("q", "khong-co-tai-lieu-nay"),
                        new int[] { 401, 403, 200, 200, 200 } },
                // test không có secret Keycloak: admin qua kiểm quyền rồi nhận 503 "chưa cấu hình"
                { "quản lý người dùng", get("/api/v1/admin/users"), new int[] { 401, 403, 403, 403, 503 } },
                { "quên mật khẩu (không lộ email)", post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ai.do@viettel.com.vn\"}"), new int[] { 202, 202, 202, 202, 202 } },
                { "đường dẫn không khai báo", get("/khong-khai-bao"), new int[] { 401, 403, 403, 403, 403 } },
        };
        JwtRequestPostProcessor[] tokens = { null, token(), token(Roles.ANALYST),
                token(Roles.DOCUMENT_MANAGER, Roles.ANALYST), token(Roles.ADMIN, Roles.DOCUMENT_MANAGER, Roles.ANALYST) };
        for (Object[] row : matrix) {
            int[] expected = (int[]) row[2];
            for (int i = 0; i < tokens.length; i++) {
                assertThat(code((MockHttpServletRequestBuilder) row[1], tokens[i]))
                        .as("%s, cột %d", row[0], i).isEqualTo(expected[i]);
            }
        }
    }

    @Test
    void meTraVaiTroVaViecDuocLam() throws Exception {
        mvc.perform(get("/api/v1/me").with(token(Roles.DOCUMENT_MANAGER, Roles.ANALYST)))
                .andExpect(jsonPath("$.roles.length()").value(2))
                .andExpect(jsonPath("$.permissions.ask").value(true))
                .andExpect(jsonPath("$.permissions.manageDocuments").value(true))
                .andExpect(jsonPath("$.permissions.manageUsers").value(false));
        // chưa được duyệt: đăng nhập được nhưng không có quyền gì
        mvc.perform(get("/api/v1/me").with(token("offline_access")))
                .andExpect(jsonPath("$.roles.length()").value(0))
                .andExpect(jsonPath("$.permissions.ask").value(false));
        mvc.perform(get("/api/v1/me").with(token(Roles.ADMIN, Roles.DOCUMENT_MANAGER, Roles.ANALYST)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissions.manageUsers").value(true));
    }
}
