package com.researchassistant.billing;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class PaystackWebhookVerifier {
    private final PaymentProperties properties;

    public PaystackWebhookVerifier(PaymentProperties properties) {
        this.properties = properties;
    }

    public boolean valid(byte[] rawBody, String signature) {
        if (signature == null || signature.isBlank() || properties.secretKey() == null || properties.secretKey().isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(properties.secretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            String expected = HexFormat.of().formatHex(mac.doFinal(rawBody));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            return false;
        }
    }
}
