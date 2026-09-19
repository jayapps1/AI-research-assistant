package com.researchassistant.identity.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.researchassistant.identity.exception.InvalidPhoneNumberException;
import org.springframework.stereotype.Component;

/**
 * Authoritative phone number normalizer and validator.
 *
 * <p>Uses Google's {@link PhoneNumberUtil} with Ghana ("GH") as the default region.
 * Validates and normalizes phone numbers into canonical E.164 format (e.g. +233542011738).
 * Supports both local Ghana formats (e.g. 0542011738, 233542011738) and international
 * E.164 formats starting with '+'.</p>
 */
@Component
public class PhoneNumberNormalizer {

    public static final String DEFAULT_REGION = "GH";
    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    /**
     * Normalizes a phone number to canonical E.164 format.
     *
     * @param raw the input phone number string
     * @return canonical E.164 formatted number, or null if input is null/blank
     * @throws InvalidPhoneNumberException if the number is malformed or invalid
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String input = raw.trim();

        // If input starts with country code without '+', e.g. "233542011738"
        // prepend '+' so the parser treats it as an international number
        String digitsOnly = input.replaceAll("[\\s\\-().]", "");
        if (digitsOnly.startsWith("233") && !input.startsWith("+")) {
            input = "+" + digitsOnly;
        }

        try {
            Phonenumber.PhoneNumber parsed = phoneUtil.parse(input, DEFAULT_REGION);

            if (!phoneUtil.isValidNumber(parsed)) {
                throw new InvalidPhoneNumberException(
                        "Invalid phone number. For Ghana numbers, please use a valid format such as 0542011738 or +233542011738."
                );
            }

            return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new InvalidPhoneNumberException(
                    "Malformed phone number: " + e.getMessage()
            );
        }
    }

    /**
     * Instance method for Spring bean injection if preferred.
     */
    public String normalizeNumber(String raw) {
        return normalize(raw);
    }
}
