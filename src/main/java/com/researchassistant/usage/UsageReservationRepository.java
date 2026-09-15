package com.researchassistant.usage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsageReservationRepository extends JpaRepository<UsageReservation, UUID> {
    Optional<UsageReservation> findByIdempotencyKey(String idempotencyKey);
}
