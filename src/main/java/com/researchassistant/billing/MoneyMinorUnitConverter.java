package com.researchassistant.billing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic converter between major currency units (e.g. GHS 20.00)
 * and Paystack minor units (e.g. 2000 pesewas).
 *
 * <p>Uses strict {@link BigDecimal} arithmetic and never converts through
 * IEEE 754 floating point types (float/double).</p>
 */
@Component
public class MoneyMinorUnitConverter {

    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of("BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA", "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF");
    private static final Set<String> THREE_DECIMAL_CURRENCIES = Set.of("BHD", "IQD", "JOD", "KWD", "OMR", "TND");

    /**
     * Converts a major-unit monetary amount to the smallest minor currency unit (e.g. pesewas, cents).
     *
     * @param amount   Authoritative amount in major units (e.g. 20.00 for GHS)
     * @param currency 3-letter ISO-4217 currency code (e.g. "GHS")
     * @return Deterministic minor-unit value (e.g. 2000)
     */
    public long toMinorUnits(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "Amount must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }

        int fractionDigits = getFractionDigits(currency);
        BigDecimal multiplier = BigDecimal.TEN.pow(fractionDigits);

        return amount
                .setScale(fractionDigits, RoundingMode.HALF_UP)
                .multiply(multiplier)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    /**
     * Converts a minor-unit integer (e.g. 2000 pesewas) back to major units (e.g. GHS 20.00).
     *
     * @param minorUnits Smallest unit integer
     * @param currency   3-letter ISO-4217 currency code
     * @return Authoritative amount with proper scale
     */
    public BigDecimal toMajorUnits(long minorUnits, String currency) {
        if (minorUnits < 0) {
            throw new IllegalArgumentException("Minor units cannot be negative: " + minorUnits);
        }

        int fractionDigits = getFractionDigits(currency);
        BigDecimal divisor = BigDecimal.TEN.pow(fractionDigits);

        return BigDecimal.valueOf(minorUnits)
                .divide(divisor, fractionDigits, RoundingMode.UNNECESSARY);
    }

    private int getFractionDigits(String currency) {
        if (currency == null || currency.isBlank()) {
            return 2; // Default to 2 decimals (GHS)
        }
        String normalized = currency.trim().toUpperCase();
        if (ZERO_DECIMAL_CURRENCIES.contains(normalized)) {
            return 0;
        }
        if (THREE_DECIMAL_CURRENCIES.contains(normalized)) {
            return 3;
        }
        try {
            return Currency.getInstance(normalized).getDefaultFractionDigits();
        } catch (IllegalArgumentException ignored) {
            return 2;
        }
    }
}
