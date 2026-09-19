package com.researchassistant.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SubscriptionRenewalReminderRepository extends JpaRepository<SubscriptionRenewalReminder, UUID> {

    boolean existsBySubscriptionIdAndReminderTypeAndCurrentPeriodEnd(
            UUID subscriptionId,
            String reminderType,
            OffsetDateTime currentPeriodEnd
    );

    List<SubscriptionRenewalReminder> findAllBySubscriptionId(UUID subscriptionId);
}
