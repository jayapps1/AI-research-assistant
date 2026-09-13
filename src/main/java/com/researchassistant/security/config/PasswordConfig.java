package com.researchassistant.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides password-related security components.
 *
 * <p>The application never stores raw passwords. A
 * {@link PasswordEncoder} transforms a user's password into a
 * secure one-way representation before persistence.</p>
 */
@Configuration
public class PasswordConfig {

    /**
     * Creates the application password encoder.
     *
     * <p>The delegating encoder allows Spring Security to store
     * the algorithm identifier together with the resulting hash,
     * making future password algorithm upgrades easier.</p>
     *
     * @return configured password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
