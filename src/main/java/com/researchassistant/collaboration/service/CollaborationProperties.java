package com.researchassistant.collaboration.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.collaboration")
public record CollaborationProperties(Duration invitationTtl) {
    public CollaborationProperties {
        invitationTtl = invitationTtl == null ? Duration.ofDays(7) : invitationTtl;
    }
}
