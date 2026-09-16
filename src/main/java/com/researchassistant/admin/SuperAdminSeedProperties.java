package com.researchassistant.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed.super-admin")
public record SuperAdminSeedProperties(
        boolean enabled,
        String email,
        String phone,
        @JsonIgnore
        String password
) {
    public String email() {
        return email == null || email.isBlank() ? "nanagyachie@gmail.com" : email;
    }

    public String phone() {
        return phone == null || phone.isBlank() ? "0542011738" : phone;
    }
}
