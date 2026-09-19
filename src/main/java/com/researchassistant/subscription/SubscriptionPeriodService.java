package com.researchassistant.subscription;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Centralized service for calculating subscription validity periods and quota reset windows.
 */
@Service
public class SubscriptionPeriodService {

    /**
     * Calculates the end of a billing period based on start time and interval.
     *
     * @param start    the period start timestamp
     * @param interval the billing interval (MONTHLY, YEARLY, NONE)
     * @return the calculated end timestamp
     */
    public OffsetDateTime calculatePeriodEnd(OffsetDateTime start, BillingInterval interval) {
        if (start == null) {
            start = OffsetDateTime.now();
        }
        if (interval == null) {
            interval = BillingInterval.NONE;
        }

        return switch (interval) {
            case YEARLY -> start.plusYears(1);
            case MONTHLY, NONE -> start.plusMonths(1);
        };
    }

    /**
     * Calculates the subscription renewal period.
     *
     * <p>If the subscription is being renewed early (i.e. existing period end is after now),
     * the new period extends from the existing period end to preserve all remaining paid days.</p>
     *
     * <p>If the subscription has already expired, the new period begins from now (activation time).</p>
     *
     * @param existingPeriodEnd the existing subscription's period end, if any
     * @param now               current timestamp (payment activation time)
     * @param interval          the billing interval
     * @return RenewalPeriod with currentPeriodStart and currentPeriodEnd
     */
    public RenewalPeriod calculateRenewalPeriod(OffsetDateTime existingPeriodEnd, OffsetDateTime now, BillingInterval interval) {
        if (now == null) {
            now = OffsetDateTime.now();
        }
        if (interval == null) {
            interval = BillingInterval.NONE;
        }

        if (existingPeriodEnd != null && existingPeriodEnd.isAfter(now)) {
            // Early renewal: preserves remaining paid days and extends from existing period end
            OffsetDateTime newEnd = calculatePeriodEnd(existingPeriodEnd, interval);
            return new RenewalPeriod(existingPeriodEnd, newEnd, true);
        } else {
            // Expired or new activation: starts from payment activation time (now)
            OffsetDateTime newEnd = calculatePeriodEnd(now, interval);
            return new RenewalPeriod(now, newEnd, false);
        }
    }

    public record RenewalPeriod(OffsetDateTime periodStart, OffsetDateTime periodEnd, boolean isEarlyRenewal) {}

    /**
     * Computes the standard calendar month window (e.g. for FREE tier quota aggregation).
     */
    public MonthlyWindow calculateMonthlyWindow(OffsetDateTime now) {
        if (now == null) {
            now = OffsetDateTime.now();
        }
        OffsetDateTime periodStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay().atOffset(now.getOffset());
        OffsetDateTime periodEnd = periodStart.plusMonths(1);
        return new MonthlyWindow(periodStart, periodEnd);
    }

    public record MonthlyWindow(OffsetDateTime start, OffsetDateTime end) {}
}
