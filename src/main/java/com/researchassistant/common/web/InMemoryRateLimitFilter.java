package com.researchassistant.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@EnableConfigurationProperties(RateLimitProperties.class)
public class InMemoryRateLimitFilter extends OncePerRequestFilter {
    private final RateLimitProperties properties;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.enabled() || !request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = clientKey(request) + ":" + bucket(request.getRequestURI());
        Window window = windows.compute(key, (ignored, existing) -> existing == null || existing.expiresAt().isBefore(Instant.now())
                ? new Window(Instant.now().plus(properties.window()), new AtomicInteger())
                : existing);
        int count = window.count().incrementAndGet();
        int limit = sensitive(request.getRequestURI()) ? properties.sensitivePerMinute() : properties.generalPerMinute();
        if (count > limit) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"errorCode\":\"RATE_LIMITED\",\"message\":\"Too many requests.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String value = forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    private String bucket(String path) {
        if (path.contains("/auth/")) return "auth";
        if (path.contains("/billing/")) return "billing";
        if (path.contains("/documents")) return "upload";
        if (path.contains("/exports")) return "export";
        if (path.contains("/ai") || path.contains("/rag")) return "ai";
        return "general";
    }

    private boolean sensitive(String path) {
        return !"general".equals(bucket(path));
    }

    private record Window(Instant expiresAt, AtomicInteger count) {}
}
