package com.researchassistant.ai.usage;

import com.researchassistant.ai.provider.AiProviderType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

@Component
public class AiProviderCostCalculator {

    public static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);
    public static final BigDecimal CREDITS_PER_USD = BigDecimal.valueOf(1_000); // 1 AI Credit = $0.001 USD

    // Fallback default pricing per million tokens
    public static final BigDecimal DEFAULT_GPT5_INPUT = new BigDecimal("0.200000");
    public static final BigDecimal DEFAULT_GPT5_CACHED = new BigDecimal("0.020000");
    public static final BigDecimal DEFAULT_GPT5_OUTPUT = new BigDecimal("1.200000");
    public static final BigDecimal DEFAULT_EMBEDDING_INPUT = new BigDecimal("0.020000");

    private final AiModelPricingRepository pricingRepository;

    public AiProviderCostCalculator(AiModelPricingRepository pricingRepository) {
        this.pricingRepository = pricingRepository;
    }

    public record CostBreakdown(
            BigDecimal inputCost,
            BigDecimal cachedInputCost,
            BigDecimal outputCost,
            BigDecimal totalCost,
            BigDecimal credits,
            String currency,
            AiCostSource source,
            String pricingVersion
    ) {}

    public CostBreakdown calculateGenerationCost(
            String provider,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            Integer cachedInputTokens
    ) {
        int inTok = inputTokens != null && inputTokens > 0 ? inputTokens : 0;
        int outTok = outputTokens != null && outputTokens > 0 ? outputTokens : 0;
        int cachedTok = cachedInputTokens != null && cachedInputTokens > 0 ? Math.min(cachedInputTokens, inTok) : 0;
        int uncachedInTok = Math.max(0, inTok - cachedTok);

        if (inTok == 0 && outTok == 0) {
            return new CostBreakdown(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, "USD", AiCostSource.CONFIGURED_PRICING, null
            );
        }

        BigDecimal inPrice = DEFAULT_GPT5_INPUT;
        BigDecimal cachedPrice = DEFAULT_GPT5_CACHED;
        BigDecimal outPrice = DEFAULT_GPT5_OUTPUT;
        String currency = "USD";
        AiCostSource source = AiCostSource.CONFIGURED_PRICING;
        String version = null;

        AiProviderType providerType = parseProviderType(provider);
        if (providerType != null && model != null) {
            Optional<AiModelPricing> pricingOpt = pricingRepository
                    .findFirstByProviderAndModelAndActiveIsTrueAndEffectiveFromBeforeOrderByEffectiveFromDesc(
                            providerType, model, OffsetDateTime.now()
                    );
            if (pricingOpt.isPresent()) {
                AiModelPricing p = pricingOpt.get();
                if (p.getInputPricePerMillion() != null) inPrice = p.getInputPricePerMillion();
                if (p.getCachedInputPricePerMillion() != null) cachedPrice = p.getCachedInputPricePerMillion();
                if (p.getOutputPricePerMillion() != null) outPrice = p.getOutputPricePerMillion();
                currency = p.getCurrency();
                version = p.getId().toString();
            } else {
                source = AiCostSource.ESTIMATED;
            }
        }

        BigDecimal inputCost = BigDecimal.valueOf(uncachedInTok)
                .multiply(inPrice)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);

        BigDecimal cachedCost = BigDecimal.valueOf(cachedTok)
                .multiply(cachedPrice)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);

        BigDecimal outputCost = BigDecimal.valueOf(outTok)
                .multiply(outPrice)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);

        BigDecimal totalCost = inputCost.add(cachedCost).add(outputCost);

        // 1 Credit = $0.001 USD -> credits = totalCost * 1000
        BigDecimal credits = totalCost.multiply(CREDITS_PER_USD).setScale(4, RoundingMode.HALF_UP);
        if (credits.compareTo(BigDecimal.ZERO) == 0 && (inTok > 0 || outTok > 0)) {
            credits = new BigDecimal("0.0001");
        }

        return new CostBreakdown(
                inputCost, cachedCost, outputCost, totalCost, credits, currency, source, version
        );
    }

    public CostBreakdown calculateEmbeddingCost(
            String provider,
            String model,
            Integer totalTokens
    ) {
        int tokens = totalTokens != null && totalTokens > 0 ? totalTokens : 0;
        if (tokens == 0) {
            return new CostBreakdown(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, "USD", AiCostSource.CONFIGURED_PRICING, null
            );
        }

        BigDecimal inPrice = DEFAULT_EMBEDDING_INPUT;
        String currency = "USD";
        AiCostSource source = AiCostSource.CONFIGURED_PRICING;
        String version = null;

        AiProviderType providerType = parseProviderType(provider);
        if (providerType != null && model != null) {
            Optional<AiModelPricing> pricingOpt = pricingRepository
                    .findFirstByProviderAndModelAndActiveIsTrueAndEffectiveFromBeforeOrderByEffectiveFromDesc(
                            providerType, model, OffsetDateTime.now()
                    );
            if (pricingOpt.isPresent()) {
                AiModelPricing p = pricingOpt.get();
                if (p.getInputPricePerMillion() != null) inPrice = p.getInputPricePerMillion();
                currency = p.getCurrency();
                version = p.getId().toString();
            } else {
                source = AiCostSource.ESTIMATED;
            }
        }


        BigDecimal totalCost = BigDecimal.valueOf(tokens)
                .multiply(inPrice)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);

        BigDecimal credits = totalCost.multiply(CREDITS_PER_USD).setScale(4, RoundingMode.HALF_UP);
        if (credits.compareTo(BigDecimal.ZERO) == 0 && tokens > 0) {
            credits = new BigDecimal("0.0001");
        }

        return new CostBreakdown(
                totalCost, BigDecimal.ZERO, BigDecimal.ZERO, totalCost, credits, currency, source, version
        );
    }

    public BigDecimal estimateCallCostUsd(String provider, String model, int estimatedInputTokens, int maxOutputTokens) {
        int inTok = Math.max(1000, estimatedInputTokens);
        int outTok = maxOutputTokens > 0 ? maxOutputTokens : 2048;
        CostBreakdown breakdown = calculateGenerationCost(provider, model, inTok, outTok, 0);
        return breakdown.totalCost();
    }

    private AiProviderType parseProviderType(String provider) {
        if (provider == null || provider.isBlank()) {
            return null;
        }
        try {
            return AiProviderType.valueOf(provider.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
