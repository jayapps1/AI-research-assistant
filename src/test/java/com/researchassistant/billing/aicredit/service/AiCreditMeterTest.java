package com.researchassistant.billing.aicredit.service;

import com.researchassistant.billing.aicredit.entity.AiCreditRate;
import com.researchassistant.billing.aicredit.repository.AiCreditRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCreditMeterTest {

    @Mock
    private AiCreditRateRepository rateRepository;

    private AiCreditMeter creditMeter;

    @BeforeEach
    void setUp() {
        AiCreditRate rate = new AiCreditRate();
        rate.setProvider("OPENAI");
        rate.setModel("gpt-5.6-luna");
        rate.setCreditsPerMillionInputTokens(new BigDecimal("3.0000"));
        rate.setCreditsPerMillionOutputTokens(new BigDecimal("15.0000"));
        rate.setActive(true);

        org.mockito.Mockito.lenient().when(rateRepository.findFirstByProviderIgnoreCaseAndModelIgnoreCaseAndActiveTrueAndEffectiveFromBeforeOrderByEffectiveFromDesc(
                anyString(), anyString(), any()))
                .thenReturn(Optional.of(rate));
        creditMeter = new AiCreditMeter(rateRepository);
    }

    @Test
    void calculateCreditsForKnownModel() {
        // gpt-5.6-luna: 3.00/1M input, 15.00/1M output
        // 1000 input tokens: 1000 * 3.00 / 1_000_000 = 0.003
        // 1000 output tokens: 1000 * 15.00 / 1_000_000 = 0.015
        // Total = 0.0180
        BigDecimal credits = creditMeter.calculateCredits("OPENAI", "gpt-5.6-luna", 1000, 1000);
        assertThat(credits).isEqualByComparingTo(new BigDecimal("0.0180"));
    }

    @Test
    void calculateCreditsWithZeroTokensReturnsZero() {
        BigDecimal credits = creditMeter.calculateCredits("OPENAI", "gpt-5.6-luna", 0, 0);
        assertThat(credits).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void estimateReservationReturnsSufficientBuffer() {
        BigDecimal estimate = creditMeter.estimateReservation("OPENAI", "gpt-5.6-luna", 1500, 4096);
        assertThat(estimate).isGreaterThan(BigDecimal.ZERO);
        // Reservation buffer is 1.25x
        BigDecimal raw = creditMeter.calculateCredits("OPENAI", "gpt-5.6-luna", 1500, 4096);
        assertThat(estimate).isGreaterThanOrEqualTo(raw);
    }
}
