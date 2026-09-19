package com.researchassistant.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyMinorUnitConverterTest {

    private MoneyMinorUnitConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MoneyMinorUnitConverter();
    }

    @Test
    void toMinorUnits_freePlan_returnsZero() {
        long minorUnits = converter.toMinorUnits(BigDecimal.ZERO, "GHS");
        assertThat(minorUnits).isEqualTo(0L);

        long minorUnitsWithDecimals = converter.toMinorUnits(new BigDecimal("0.00"), "GHS");
        assertThat(minorUnitsWithDecimals).isEqualTo(0L);
    }

    @Test
    void toMinorUnits_studentMonthlyPrice_returns2000MinorUnits() {
        // GHS 20.00 -> 2000 pesewas
        long minorUnits = converter.toMinorUnits(new BigDecimal("20.00"), "GHS");
        assertThat(minorUnits).isEqualTo(2000L);

        long minorUnitsInteger = converter.toMinorUnits(new BigDecimal("20"), "GHS");
        assertThat(minorUnitsInteger).isEqualTo(2000L);
    }

    @Test
    void toMinorUnits_proMonthlyPrice_returns10000MinorUnits() {
        // GHS 100.00 -> 10000 pesewas
        long minorUnits = converter.toMinorUnits(new BigDecimal("100.00"), "GHS");
        assertThat(minorUnits).isEqualTo(10000L);

        long minorUnitsInteger = converter.toMinorUnits(new BigDecimal("100"), "GHS");
        assertThat(minorUnitsInteger).isEqualTo(10000L);
    }

    @Test
    void toMajorUnits_convertsMinorUnitsBackToAuthoritativeBigDecimal() {
        BigDecimal studentPrice = converter.toMajorUnits(2000L, "GHS");
        assertThat(studentPrice).isEqualByComparingTo("20.00");

        BigDecimal proPrice = converter.toMajorUnits(10000L, "GHS");
        assertThat(proPrice).isEqualByComparingTo("100.00");

        BigDecimal freePrice = converter.toMajorUnits(0L, "GHS");
        assertThat(freePrice).isEqualByComparingTo("0.00");
    }

    @Test
    void toMinorUnits_rejectsNegativeAmounts() {
        assertThatThrownBy(() -> converter.toMinorUnits(new BigDecimal("-20.00"), "GHS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount cannot be negative");
    }

    @Test
    void toMajorUnits_rejectsNegativeMinorUnits() {
        assertThatThrownBy(() -> converter.toMajorUnits(-100L, "GHS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minor units cannot be negative");
    }

    @Test
    void toMinorUnits_handlesDifferentCurrencies() {
        // JPY has 0 fraction digits
        long jpy = converter.toMinorUnits(new BigDecimal("500"), "JPY");
        assertThat(jpy).isEqualTo(500L);

        // BHD has 3 fraction digits
        long bhd = converter.toMinorUnits(new BigDecimal("12.500"), "BHD");
        assertThat(bhd).isEqualTo(12500L);
    }
}
