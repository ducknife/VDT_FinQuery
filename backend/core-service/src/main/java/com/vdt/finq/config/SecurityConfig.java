package com.vdt.finq.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import com.vdt.finq.common.security.Roles;

import jakarta.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;

// core là resource server (QĐ-038): kiểm token Keycloak, đọc vai trò; không tự quản lý người dùng.
// finq.security.enabled=false (mặc định khi chưa có Keycloak): mở mọi API, người dùng là "dev" đủ quyền
@Slf4j
@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    @Value("${finq.security.enabled:false}")
    private boolean enabled;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (!enabled) {
            log.warn("finq.security.enabled=false: API mở, không kiểm token");
            http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
            return http.build();
        }
        // bảng quyền (QĐ-043, xem Roles); quy tắc đầu tiên khớp được áp dụng
        http.authorizeHttpRequests(a -> a
                // SSE trả kết quả qua async dispatch: request gốc đã được kiểm
                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                // quên mật khẩu: chưa đăng nhập nên mở; core luôn trả cùng một câu (không lộ email nào có tài khoản)
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password").permitAll()
                // ai đăng nhập cũng xem được mình là ai, kể cả tài khoản chưa được duyệt (giao diện báo "chờ duyệt")
                .requestMatchers(HttpMethod.GET, "/api/v1/me").authenticated()
                // tự sửa họ tên, đổi mật khẩu của chính mình
                .requestMatchers("/api/v1/me/**").authenticated()
                // quản lý tài khoản (qua Keycloak Admin API): chỉ admin
                .requestMatchers("/api/v1/admin/**").hasRole(Roles.ADMIN)
                // nạp, index, trích số tài liệu; thử truy hồi: document_manager, admin
                .requestMatchers(HttpMethod.POST, "/api/v1/documents/**").hasAnyRole(Roles.DOCUMENTS)
                .requestMatchers("/api/v1/retrieval/**").hasAnyRole(Roles.DOCUMENTS)
                // hỏi đáp, trích dẫn, trang tài liệu, hội thoại, feedback: cả ba vai trò
                .requestMatchers("/api/**").hasAnyRole(Roles.ANY)
                // API thử của bộ làm sạch: chỉ admin
                .requestMatchers("/test/**").hasRole(Roles.ADMIN)
                // đường dẫn không khai báo: chặn
                .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(keycloakRoles())));
        return http.build();
    }

    // vai trò realm của Keycloak (realm_access.roles) thành ROLE_<tên>
    static Converter<Jwt, AbstractAuthenticationToken> keycloakRoles() {
        return jwt -> {
            Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
            Collection<?> roles = realm == null ? List.of() : (Collection<?>) realm.getOrDefault("roles", List.of());
            List<GrantedAuthority> authorities = roles.stream()
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r)).toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("preferred_username"));
        };
    }
}
