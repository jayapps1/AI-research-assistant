package com.researchassistant.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.api")
public record ApiSecurityProperties(
        List<String> allowedOrigins,
        int maxPageSize,
        boolean hstsEnabled
) {
    public List<String> allowedOrigins() {
        return allowedOrigins == null ? List.of("http://localhost:3000", "http://localhost:5173") : allowedOrigins;
    }

    public int maxPageSize() {
        return maxPageSize <= 0 ? 100 : Math.min(maxPageSize, 500);
    }
}
