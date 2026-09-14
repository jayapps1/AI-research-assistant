package com.researchassistant.cache;

import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CacheInvalidationService {

    private final CacheManager cacheManager;
    private final SecurityAuditService auditService;

    public CacheInvalidationService(
            CacheManager cacheManager,
            SecurityAuditService auditService
    ) {
        this.cacheManager = cacheManager;
        this.auditService = auditService;
    }

    public void evictProjectMetadata(UUID projectId) {
        clear(AppCacheNames.PROJECT_METADATA);
    }

    public void evictAllProjectMetadata() {
        clear(AppCacheNames.PROJECT_METADATA);
    }

    public void evictDocumentMetadata(UUID documentId, UUID projectId) {
        clear(AppCacheNames.DOCUMENT_METADATA);
        clear(AppCacheNames.PROJECT_METADATA);
    }

    public void evictResearchDesignValidation(UUID projectId) {
        clear(AppCacheNames.RESEARCH_DESIGN_VALIDATION);
    }

    public void evictLiteratureSummary(UUID projectId) {
        clear(AppCacheNames.LITERATURE_SUMMARY);
    }

    public void flushApplicationCaches(UUID actorUserId) {
        for (String cacheName : cacheManager.getCacheNames()) {
            clear(cacheName);
        }
        auditService.record(actorUserId, SecurityAuditEventType.CACHE_FLUSHED);
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
