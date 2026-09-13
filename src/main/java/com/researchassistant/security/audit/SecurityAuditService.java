package com.researchassistant.security.audit;

import java.util.UUID;

/**
 * Boundary for recording security-sensitive events.
 *
 * <p>Implementations must never record passwords, TOTP codes,
 * TOTP secrets, recovery-code plaintext, refresh tokens, reset
 * tokens or encryption keys.</p>
 */
public interface SecurityAuditService {

    void record(UUID userId, SecurityAuditEventType eventType);
}
