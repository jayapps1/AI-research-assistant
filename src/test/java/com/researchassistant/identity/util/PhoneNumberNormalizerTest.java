package com.researchassistant.identity.util;

import com.researchassistant.identity.exception.InvalidPhoneNumberException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberNormalizerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "0542011738",
            "054 201 1738",
            "054-201-1738",
            "233542011738",
            "+233542011738",
            "+233 54 201 1738"
    })
    void normalizesEquivalentGhanaInputsToCanonicalE164(String input) {
        String normalized = PhoneNumberNormalizer.normalize(input);
        assertThat(normalized).isEqualTo("+233542011738");
    }

    @Test
    void normalizationIsIdempotent() {
        String first = PhoneNumberNormalizer.normalize("0542011738");
        String second = PhoneNumberNormalizer.normalize(first);
        assertThat(second).isEqualTo("+233542011738");
        assertThat(second).isEqualTo(first);
    }

    @Test
    void normalizesOtherValidGhanaNumbers() {
        // Vodafone/Telecel Ghana: 0201234567
        assertThat(PhoneNumberNormalizer.normalize("0201234567")).isEqualTo("+233201234567");
        // AirtelTigo: 0271234567
        assertThat(PhoneNumberNormalizer.normalize("0271234567")).isEqualTo("+233271234567");
    }

    @Test
    void supportsInternationalNumbersStartingWithPlus() {
        // US number
        assertThat(PhoneNumberNormalizer.normalize("+14155552671")).isEqualTo("+14155552671");
        // UK number
        assertThat(PhoneNumberNormalizer.normalize("+447911123456")).isEqualTo("+447911123456");
    }

    @Test
    void returnsNullForNullOrBlank() {
        assertThat(PhoneNumberNormalizer.normalize(null)).isNull();
        assertThat(PhoneNumberNormalizer.normalize("")).isNull();
        assertThat(PhoneNumberNormalizer.normalize("   ")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345",              // too short
            "0542011",            // incomplete Ghana number
            "054201173899999",    // too long
            "invalid-phone",      // non-numeric
            "+999999999999999",   // invalid country code
            "0000000000"          // invalid national number
    })
    void rejectsInvalidNumbersWithStructuredValidationError(String invalidInput) {
        assertThatThrownBy(() -> PhoneNumberNormalizer.normalize(invalidInput))
                .isInstanceOf(InvalidPhoneNumberException.class);
    }
}
