package com.vdt.finq.modules.account;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.properties.KeycloakProperties;

import lombok.extern.slf4j.Slf4j;

// gọi Keycloak Admin REST API của realm finquery bằng token client_credentials của finquery-core.
// chỉ những thao tác FinQuery cần: tìm, tạo, sửa người dùng, vai trò realm, gửi mail thao tác (đặt mật khẩu)
@Slf4j
@Component
public class KeycloakAdminClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
    };

    private final KeycloakProperties properties;
    private final ObjectMapper json;
    private final RestClient http;
    private String token;
    private Instant tokenExpiry = Instant.EPOCH;

    public KeycloakAdminClient(KeycloakProperties properties, ObjectMapper json) {
        this.properties = properties;
        this.json = json;
        this.http = RestClient.builder().baseUrl(properties.getUrl())
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                    throw new AppException(res.getStatusCode().value(), keycloakError(res.getBody().readAllBytes()));
                })
                .build();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public List<Map<String, Object>> users(String search, int first, int max) {
        return http.get().uri(b -> b.path(admin("/users")).queryParam("briefRepresentation", false)
                .queryParam("first", first).queryParam("max", max)
                .queryParamIfPresent("search", Optional.ofNullable(search).filter(s -> !s.isBlank())).build())
                .headers(h -> h.setBearerAuth(token())).retrieve().body(LIST);
    }

    public Optional<Map<String, Object>> findByEmail(String email) {
        List<Map<String, Object>> found = http.get().uri(b -> b.path(admin("/users")).queryParam("email", email)
                .queryParam("exact", true).build()).headers(h -> h.setBearerAuth(token())).retrieve().body(LIST);
        return found == null ? Optional.empty() : found.stream().findFirst();
    }

    // trả về id người dùng mới (lấy từ header Location)
    public String createUser(Map<String, Object> user) {
        URI location = http.post().uri(admin("/users")).headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON).body(user).retrieve().toBodilessEntity().getHeaders().getLocation();
        if (location == null) {
            throw new AppException(502, "Keycloak không trả mã người dùng vừa tạo");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public void updateUser(String id, Map<String, Object> fields) {
        http.put().uri(admin("/users/{id}"), id).headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON).body(fields).retrieve().toBodilessEntity();
    }

    public void deleteUser(String id) {
        http.delete().uri(admin("/users/{id}"), id).headers(h -> h.setBearerAuth(token())).retrieve().toBodilessEntity();
    }

    // vai trò realm gán trực tiếp (không mở rộng vai trò lồng nhau)
    public List<Map<String, Object>> realmRoles(String id) {
        return http.get().uri(admin("/users/{id}/role-mappings/realm"), id).headers(h -> h.setBearerAuth(token()))
                .retrieve().body(LIST);
    }

    public Map<String, Object> role(String name) {
        return http.get().uri(admin("/roles/{name}"), name).headers(h -> h.setBearerAuth(token())).retrieve().body(MAP);
    }

    public void addRealmRoles(String id, List<Map<String, Object>> roles) {
        http.post().uri(admin("/users/{id}/role-mappings/realm"), id).headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON).body(roles).retrieve().toBodilessEntity();
    }

    public void removeRealmRoles(String id, List<Map<String, Object>> roles) {
        http.method(org.springframework.http.HttpMethod.DELETE).uri(admin("/users/{id}/role-mappings/realm"), id)
                .headers(h -> h.setBearerAuth(token())).contentType(MediaType.APPLICATION_JSON).body(roles).retrieve()
                .toBodilessEntity();
    }

    // đặt mật khẩu mới (không tạm); Keycloak kiểm chính sách mật khẩu, vi phạm thì trả 400
    public void resetPassword(String id, String password) {
        http.put().uri(admin("/users/{id}/reset-password"), id).headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "password", "value", password, "temporary", false)).retrieve().toBodilessEntity();
    }

    // kiểm mật khẩu hiện tại bằng cách xin token cho client frontend (sai thì Keycloak trả 401 invalid_grant)
    public boolean passwordMatches(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", properties.getFrontendClientId());
        form.add("username", username);
        form.add("password", password);
        try {
            http.post().uri("/realms/{realm}/protocol/openid-connect/token", properties.getRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().toBodilessEntity();
            return true;
        } catch (AppException e) {
            if (e.getErrorCode() == 401 || e.getErrorCode() == 400) {
                return false;
            }
            throw e;
        }
    }

    // Keycloak gửi mail chứa link thao tác (ví dụ UPDATE_PASSWORD), xong thì quay về frontend
    public void sendActionsEmail(String id, List<String> actions) {
        http.put().uri(b -> b.path(admin("/users/{id}/execute-actions-email"))
                .queryParam("client_id", properties.getFrontendClientId())
                .queryParam("redirect_uri", properties.getFrontendUrl())
                .queryParam("lifespan", properties.getActionLifespanSeconds()).build(id))
                .headers(h -> h.setBearerAuth(token())).contentType(MediaType.APPLICATION_JSON).body(actions).retrieve()
                .toBodilessEntity();
    }

    private String admin(String path) {
        return "/admin/realms/" + properties.getRealm() + path;
    }

    // token của tài khoản dịch vụ, dùng lại tới khi còn dưới 30 giây
    private synchronized String token() {
        if (!properties.isConfigured()) {
            throw new AppException(503, "Chưa cấu hình KEYCLOAK_CORE_CLIENT_SECRET cho core");
        }
        if (token != null && Instant.now().isBefore(tokenExpiry.minusSeconds(30))) {
            return token;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        JsonNode body = http.post().uri("/realms/{realm}/protocol/openid-connect/token", properties.getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
        if (body == null || !body.hasNonNull("access_token")) {
            throw new AppException(502, "Keycloak không cấp token cho core");
        }
        token = body.get("access_token").asText();
        tokenExpiry = Instant.now().plusSeconds(body.path("expires_in").asLong(60));
        return token;
    }

    // lỗi Keycloak: {"errorMessage": "..."} hoặc {"error_description": "..."}; trùng email trả 409
    private String keycloakError(byte[] body) {
        try {
            JsonNode n = json.readTree(body);
            for (String k : List.of("errorMessage", "error_description", "error")) {
                if (n.hasNonNull(k)) {
                    return "Keycloak: " + n.get(k).asText();
                }
            }
        } catch (Exception ignored) {
            // không phải JSON
        }
        return "Keycloak trả lỗi";
    }
}
