package com.researchassistant.billing.aicredit.service;

import com.researchassistant.billing.aicredit.entity.AiCreditRate;
import com.researchassistant.billing.aicredit.repository.AiCreditRateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class AiCreditMeter {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);
    // Default fallback rates per million tokens (1 Credit = $0.001 USD)
    // gpt-5.6-luna defaults: Input $0.20/M -> 200 credits/M; Output $1.20/M -> 1200 credits/M; Cached $0.02/M -> 20 credits/M
    private static final BigDecimal DEFAULT_INPUT_RATE = new BigDecimal("200.0000");
    private static final BigDecimal DEFAULT_CACHED_RATE = new BigDecimal("20.0000");
    private static final BigDecimal DEFAULT_OUTPUT_RATE = new BigDecimal("1200.0000");
    private static final BigDecimal RESERVATION_BUFFER_MULTIPLIER = new BigDecimal("1.25");

    private final AiCreditRateRepository rateRepository;

    public AiCreditMeter(AiCreditRateRepository rateRepository) {
        this.rateRepository = rateRepository;
    }

    /**
     * Calculates the actual AI credits consumed for a given request usage without cached tokens.
     */
    public BigDecimal calculateCredits(String provider, String model, Integer inputTokens, Integer outputTokens) {
        return calculateCredits(provider, model, inputTokens, outputTokens, 0);
    }

    /**
     * Calculates the actual AI credits consumed for a given request usage including cached tokens.
     */
    public BigDecimal calculateCredits(String provider, String model, Integer inputTokens, Integer outputTokens, Integer cachedInputTokens) {
        int inTokens = inputTokens != null && inputTokens > 0 ? inputTokens : 0;
        int outTokens = outputTokens != null && outputTokens > 0 ? outputTokens : 0;
        int cachedTokens = cachedInputTokens != null && cachedInputTokens > 0 ? Math.min(cachedInputTokens, inTokens) : 0;
        int uncachedTokens = Math.max(0, inTokens - cachedTokens);

        if (inTokens == 0 && outTokens == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal inputRate = DEFAULT_INPUT_RATE;
        BigDecimal cachedRate = DEFAULT_CACHED_RATE;
        BigDecimal outputRate = DEFAULT_OUTPUT_RATE;

        if (provider != null && model != null) {
            Optional<AiCreditRate> rateOpt = rateRepository
                    .findFirstByProviderIgnoreCaseAndModelIgnoreCaseAndActiveTrueAndEffectiveFromBeforeOrderByEffectiveFromDesc(
                            provider, model, OffsetDateTime.now()
                    );
            if (rateOpt.isPresent()) {
                inputRate = rateOpt.get().getCreditsPerMillionInputTokens();
                outputRate = rateOpt.get().getCreditsPerMillionOutputTokens();
                if (rateOpt.get().getCreditsPerMillionCachedInputTokens() != null) {
                    cachedRate = rateOpt.get().getCreditsPerMillionCachedInputTokens();
                }
            }
        }

        BigDecimal uncachedCost = BigDecimal.valueOf(uncachedTokens)
                .multiply(inputRate)
                .divide(ONE_MILLION, 4, RoundingMode.HALF_UP);

        BigDecimal cachedCost = BigDecimal.valueOf(cachedTokens)
                .multiply(cachedRate)
                .divide(ONE_MILLION, 4, RoundingMode.HALF_UP);

        BigDecimal outputCost = BigDecimal.valueOf(outTokens)
                .multiply(outputRate)
                .divide(ONE_MILLION, 4, RoundingMode.HALF_UP);

        BigDecimal total = uncachedCost.add(cachedCost).add(outputCost);
        // Minimum non-zero charge is 0.0001 credit if any tokens were used
        if (total.compareTo(BigDecimal.ZERO) == 0 && (inTokens > 0 || outTokens > 0)) {
            return new BigDecimal("0.0001");
        }
        return total;
    }

    /**
     * Estimates the maximum credits required for reservation before calling provider.
     */
    public BigDecimal estimateReservation(String provider, String model, int estimatedInputTokens, int maxOutputTokens) {
        int inTokens = Math.max(500, estimatedInputTokens);
        int outTokens = maxOutputTokens > 0 ? maxOutputTokens : 4096;
        return calculateCredits(provider, model, inTokens, outTokens, 0)
                .multiply(RESERVATION_BUFFER_MULTIPLIER)
                .setScale(4, RoundingMode.HALF_UP);
    }
}
