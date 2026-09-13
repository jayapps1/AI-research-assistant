package com.researchassistant.security.audit;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Placeholder audit boundary until durable audit storage is added.
 */
@Service
public class NoOpSecurityAuditService implements SecurityAuditService {

    @Override
    public void record(UUID userId, SecurityAuditEventType eventType) {
        // Intentionally no-op; preserves the audit boundary without
        // risking accidental credential logging.
    }
}
