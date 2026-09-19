package com.researchassistant.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionPeriodServiceTest {

    private SubscriptionPeriodService periodService;

    @BeforeEach
    void setUp() {
        periodService = new SubscriptionPeriodService();
    }

    @Test
    void calculatePeriodEnd_monthly_createsOneMonthPeriodWithCalendarArithmetic() {
        // User pays on 19 September 2026 -> 19 October 2026
        OffsetDateTime start = OffsetDateTime.of(2026, 9, 19, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = periodService.calculatePeriodEnd(start, BillingInterval.MONTHLY);

        assertThat(end).isEqualTo(OffsetDateTime.of(2026, 10, 19, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void calculatePeriodEnd_monthly_handlesMonthEndCorrectly() {
        // 31 January -> 28 February (in non-leap year)
        OffsetDateTime start = OffsetDateTime.of(2027, 1, 31, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = periodService.calculatePeriodEnd(start, BillingInterval.MONTHLY);

        assertThat(end).isEqualTo(OffsetDateTime.of(2027, 2, 28, 12, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void calculatePeriodEnd_yearly_createsOneYearPeriod() {
        // User pays on 19 September 2026 -> 19 September 2027
        OffsetDateTime start = OffsetDateTime.of(2026, 9, 19, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = periodService.calculatePeriodEnd(start, BillingInterval.YEARLY);

        assertThat(end).isEqualTo(OffsetDateTime.of(2027, 9, 19, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void calculateRenewalPeriod_earlyRenewal_extendsFromExistingPeriodEnd() {
        // Current PRO expires: 19 October 2026
        // User renews early on: 15 October 2026
        OffsetDateTime existingPeriodEnd = OffsetDateTime.of(2026, 10, 19, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime now = OffsetDateTime.of(2026, 10, 15, 14, 30, 0, 0, ZoneOffset.UTC);

        SubscriptionPeriodService.RenewalPeriod renewal = periodService.calculateRenewalPeriod(
                existingPeriodEnd, now, BillingInterval.MONTHLY
        );

        // Does NOT discard the remaining 4 paid days: period extends from 19 Oct -> 19 Nov
        assertThat(renewal.isEarlyRenewal()).isTrue();
        assertThat(renewal.periodStart()).isEqualTo(existingPeriodEnd);
        assertThat(renewal.periodEnd()).isEqualTo(OffsetDateTime.of(2026, 11, 19, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void calculateRenewalPeriod_expiredRenewal_startsFromPaymentTime() {
        // Current PRO expired on: 19 October 2026
        // User renews after expiry on: 22 October 2026
        OffsetDateTime existingPeriodEnd = OffsetDateTime.of(2026, 10, 19, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime now = OffsetDateTime.of(2026, 10, 22, 9, 0, 0, 0, ZoneOffset.UTC);

        SubscriptionPeriodService.RenewalPeriod renewal = periodService.calculateRenewalPeriod(
                existingPeriodEnd, now, BillingInterval.MONTHLY
        );

        // Starts from verified payment time
        assertThat(renewal.isEarlyRenewal()).isFalse();
        assertThat(renewal.periodStart()).isEqualTo(now);
        assertThat(renewal.periodEnd()).isEqualTo(OffsetDateTime.of(2026, 11, 22, 9, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void calculateRenewalPeriod_yearlyEarlyRenewal_extendsFromExistingEnd() {
        // Current PRO yearly expires: 19 September 2027
        // User renews on: 10 September 2027
        OffsetDateTime existingPeriodEnd = OffsetDateTime.of(2027, 9, 19, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime now = OffsetDateTime.of(2027, 9, 10, 8, 0, 0, 0, ZoneOffset.UTC);

        SubscriptionPeriodService.RenewalPeriod renewal = periodService.calculateRenewalPeriod(
                existingPeriodEnd, now, BillingInterval.YEARLY
        );

        assertThat(renewal.isEarlyRenewal()).isTrue();
        assertThat(renewal.periodStart()).isEqualTo(existingPeriodEnd);
        assertThat(renewal.periodEnd()).isEqualTo(OffsetDateTime.of(2028, 9, 19, 10, 0, 0, 0, ZoneOffset.UTC));
    }
}
