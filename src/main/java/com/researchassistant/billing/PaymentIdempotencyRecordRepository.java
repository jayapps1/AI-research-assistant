package com.researchassistant.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIdempotencyRecordRepository extends JpaRepository<PaymentIdempotencyRecord, UUID> {
    Optional<PaymentIdempotencyRecord> findByScopeAndIdempotencyKey(String scope, String idempotencyKey);
}
