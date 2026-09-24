package com.researchassistant.rag.service;

import org.springframework.stereotype.Component;

@Component
public class TokenEstimator {

    public int estimate(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        int chars = value.strip().length();
        return Math.max(1, (int) Math.ceil(chars / 4.0));
    }
}
