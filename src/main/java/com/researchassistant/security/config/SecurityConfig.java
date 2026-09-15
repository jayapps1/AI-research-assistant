package com.researchassistant.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.researchassistant.common.config.ApiSecurityProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Defines the HTTP security policy for the Research Assistant API.
 *
 * <p>The application is designed as a stateless REST API. Browser
 * sessions are therefore not used for authentication. Protected
 * endpoints accept OAuth2 Resource Server JWT bearer tokens.</p>
 *
 * <p>Only explicitly public endpoints are currently accessible
 * without authentication. All other endpoints remain protected.</p>
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures username/password authentication against the
     * application user store. Password verification remains inside
     * Spring Security so raw credentials are not compared manually.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }


    /**
     * Exposes Spring Security's authentication manager for the
     * login endpoint.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configures the application's primary Spring Security filter chain.
     *
     * <p>CSRF protection is disabled because the backend is designed
     * as a stateless token-based API rather than a server-rendered
     * application using browser session cookies.</p>
     *
     * @param http Spring Security HTTP configuration
     * @return configured security filter chain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider
    )
            throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(authorize ->
                        authorize

                                // ------------------------------------------------
                                // Public authentication endpoints
                                // ------------------------------------------------
                                .requestMatchers(
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/auth/password/forgot",
                                        "/api/v1/auth/password/verify-totp",
                                        "/api/v1/auth/password/verify-recovery-code",
                                        "/api/v1/auth/logout",
                                        "/api/v1/auth/password/reset",
                                        "/api/v1/billing/webhooks/paystack",
                                        "/api/v1/billing/paystack/callback"
                                ).permitAll()

                                // ------------------------------------------------
                                // OpenAPI / Swagger documentation
                                // ------------------------------------------------
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                ).permitAll()

                                // ------------------------------------------------
                                // Basic application health endpoint
                                // ------------------------------------------------
                                .requestMatchers(
                                        "/actuator/health",
                                        "/actuator/info"
                                ).permitAll()

                                // ------------------------------------------------
                                // Everything else requires authentication.
                                // ------------------------------------------------
                                .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider)

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(ApiSecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Request-ID"));
        configuration.setExposedHeaders(List.of("X-Request-ID"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
