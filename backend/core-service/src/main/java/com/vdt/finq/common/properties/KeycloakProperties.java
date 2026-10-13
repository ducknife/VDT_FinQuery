package com.vdt.finq.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// core gọi API quản trị của Keycloak bằng tài khoản dịch vụ finquery-core (quản lý người dùng, mail đặt lại mật khẩu)
@Configuration
@ConfigurationProperties(prefix = "finq.keycloak")
@Getter
@Setter
@NoArgsConstructor
public class KeycloakProperties {
    private String url = "http://localhost:8180";
    private String realm = "finquery";
    private String clientId = "finquery-core";
    // do Keycloak sinh (Clients -> finquery-core -> Credentials), đặt qua biến môi trường; trống thì tắt tính năng
    private String clientSecret = "";
    // client của frontend và trang quay về sau khi đặt lại mật khẩu từ link trong mail
    private String frontendClientId = "finquery-web";
    private String frontendUrl = "http://localhost:5173/";
    // link trong mail sống bao lâu
    private int actionLifespanSeconds = 900;

    public boolean isConfigured() {
        return clientSecret != null && !clientSecret.isBlank();
    }
}
