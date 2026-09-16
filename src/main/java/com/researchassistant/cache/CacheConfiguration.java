package com.researchassistant.cache;

import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "app.cache",
            name = "provider",
            havingValue = "simple",
            matchIfMissing = true
    )
    CacheManager simpleCacheManager() {
        ConcurrentMapCacheManager cacheManager =
                new ConcurrentMapCacheManager(
                        AppCacheNames.PROJECT_METADATA,
                        AppCacheNames.DOCUMENT_METADATA,
                        AppCacheNames.LITERATURE_SUMMARY,
                        AppCacheNames.RESEARCH_DESIGN_VALIDATION,
                        AppCacheNames.FORMATTED_CITATIONS,
                        AppCacheNames.PUBLIC_SITE_SETTINGS,
                        AppCacheNames.PUBLIC_PAGES,
                        AppCacheNames.PUBLIC_SERVICES,
                        AppCacheNames.PUBLIC_FAQS,
                        AppCacheNames.PUBLIC_PRICING
                );
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "app.cache",
            name = "provider",
            havingValue = "none"
    )
    CacheManager noOpCacheManager() {
        return new NoOpCacheManager();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "app.cache",
            name = "provider",
            havingValue = "redis"
    )
    CacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            AppCacheProperties properties
    ) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaults = RedisCacheConfiguration
                .defaultCacheConfig()
                .prefixCacheNameWith(properties.keyPrefix())
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                );

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                AppCacheNames.PROJECT_METADATA,
                defaults.entryTtl(properties.projectMetadataTtl()),
                AppCacheNames.DOCUMENT_METADATA,
                defaults.entryTtl(properties.documentMetadataTtl()),
                AppCacheNames.LITERATURE_SUMMARY,
                defaults.entryTtl(properties.literatureSummaryTtl()),
                AppCacheNames.RESEARCH_DESIGN_VALIDATION,
                defaults.entryTtl(properties.researchDesignValidationTtl()),
                AppCacheNames.FORMATTED_CITATIONS,
                defaults.entryTtl(properties.literatureSummaryTtl()),
                AppCacheNames.PUBLIC_SITE_SETTINGS,
                defaults.entryTtl(properties.projectMetadataTtl()),
                AppCacheNames.PUBLIC_PAGES,
                defaults.entryTtl(properties.projectMetadataTtl()),
                AppCacheNames.PUBLIC_SERVICES,
                defaults.entryTtl(properties.projectMetadataTtl()),
                AppCacheNames.PUBLIC_FAQS,
                defaults.entryTtl(properties.projectMetadataTtl()),
                AppCacheNames.PUBLIC_PRICING,
                defaults.entryTtl(properties.projectMetadataTtl())
        );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean
    CacheErrorHandler cacheErrorHandler(MeterRegistry meterRegistry) {
        return new MeteredCacheErrorHandler(meterRegistry);
    }

    private static final class MeteredCacheErrorHandler
            implements CacheErrorHandler {

        private static final Logger log =
                LoggerFactory.getLogger(MeteredCacheErrorHandler.class);

        private final MeterRegistry meterRegistry;

        private MeteredCacheErrorHandler(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        @Override
        public void handleCacheGetError(
                RuntimeException exception,
                org.springframework.cache.Cache cache,
                Object key
        ) {
            recordFailure("get", cache.getName(), exception);
        }

        @Override
        public void handleCachePutError(
                RuntimeException exception,
                org.springframework.cache.Cache cache,
                Object key,
                Object value
        ) {
            recordFailure("put", cache.getName(), exception);
        }

        @Override
        public void handleCacheEvictError(
                RuntimeException exception,
                org.springframework.cache.Cache cache,
                Object key
        ) {
            recordFailure("evict", cache.getName(), exception);
        }

        @Override
        public void handleCacheClearError(
                RuntimeException exception,
                org.springframework.cache.Cache cache
        ) {
            recordFailure("clear", cache.getName(), exception);
        }

        private void recordFailure(
                String operation,
                String cacheName,
                RuntimeException exception
        ) {
            meterRegistry.counter("research.cache.operation.failures").increment();
            log.warn(
                    "Cache {} failed for cache {}: {}",
                    operation,
                    cacheName,
                    exception.getClass().getSimpleName()
            );
        }
    }
}
