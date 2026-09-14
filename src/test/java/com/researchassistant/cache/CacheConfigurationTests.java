package com.researchassistant.cache;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.NoOpCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigurationTests {

    @Test
    void simpleCacheManagerWorksWithoutRedis() {
        CacheManager cacheManager = new CacheConfiguration().simpleCacheManager();

        assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
        assertThat(cacheManager.getCache(AppCacheNames.PROJECT_METADATA))
                .isNotNull();
        assertThat(cacheManager.getCache(AppCacheNames.DOCUMENT_METADATA))
                .isNotNull();
    }

    @Test
    void noneProviderUsesNoOpCacheManager() {
        CacheManager cacheManager = new CacheConfiguration().noOpCacheManager();

        assertThat(cacheManager).isInstanceOf(NoOpCacheManager.class);
    }

    @Test
    void cacheErrorHandlerCountsFailuresWithoutThrowing() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var handler = new CacheConfiguration().cacheErrorHandler(registry);
        CacheManager cacheManager = new CacheConfiguration().simpleCacheManager();

        handler.handleCacheGetError(
                new RuntimeException("redis unavailable"),
                cacheManager.getCache(AppCacheNames.PROJECT_METADATA),
                "project:1"
        );

        assertThat(registry.counter("research.cache.operation.failures").count())
                .isEqualTo(1.0);
    }
}
