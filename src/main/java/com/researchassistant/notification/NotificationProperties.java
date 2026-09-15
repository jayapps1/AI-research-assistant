package com.researchassistant.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        Email email,
        Arkesel arkesel,
        Firebase firebase
) {
    public record Email(boolean enabled, String fromAddress, String fromName) {}
    public record Arkesel(boolean enabled, String apiKey, String senderId) {}
    public record Firebase(boolean enabled, String projectId, String accessToken) {}
}
