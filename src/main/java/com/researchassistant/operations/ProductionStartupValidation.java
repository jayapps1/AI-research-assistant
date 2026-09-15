package com.researchassistant.operations;

import com.researchassistant.common.config.ApiSecurityProperties;
import com.researchassistant.billing.PaymentEnvironment;
import com.researchassistant.billing.PaymentProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ProductionStartupValidation implements ApplicationRunner {
    private final Environment environment;
    private final ApiSecurityProperties apiProperties;
    private final PaymentProperties paymentProperties;

    public ProductionStartupValidation(Environment environment, ApiSecurityProperties apiProperties, PaymentProperties paymentProperties) {
        this.environment = environment;
        this.apiProperties = apiProperties;
        this.paymentProperties = paymentProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("production")) {
            return;
        }
        String jwtSecret = environment.getProperty("app.security.jwt.secret", "");
        if (jwtSecret.length() < 32 || jwtSecret.contains("0123456789abcdef")) {
            throw new IllegalStateException("Production JWT secret is missing or unsafe.");
        }
        if (apiProperties.allowedOrigins().contains("*")) {
            throw new IllegalStateException("Production CORS allowed origins must not contain wildcard origins.");
        }
        if (paymentProperties.mode() == PaymentEnvironment.LIVE && !paymentProperties.liveEnabled()) {
            throw new IllegalStateException("Paystack LIVE mode is disabled by policy.");
        }
    }
}
