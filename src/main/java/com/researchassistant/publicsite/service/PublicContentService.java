package com.researchassistant.publicsite.service;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.audit.AuditEventType;
import com.researchassistant.cache.AppCacheNames;
import com.researchassistant.common.exception.DuplicateResourceException;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.publicsite.dto.*;
import com.researchassistant.publicsite.entity.*;
import com.researchassistant.publicsite.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PublicContentService {

    private final PublicSiteSettingsRepository settingsRepository;
    private final PublicPageRepository pageRepository;
    private final PublicPageSectionRepository sectionRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final FaqItemRepository faqRepository;
    private final AuditEventService auditEventService;

    public PublicContentService(PublicSiteSettingsRepository settingsRepository,
                                PublicPageRepository pageRepository,
                                PublicPageSectionRepository sectionRepository,
                                ServiceOfferingRepository serviceRepository,
                                FaqItemRepository faqRepository,
                                AuditEventService auditEventService) {
        this.settingsRepository = settingsRepository;
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
        this.serviceRepository = serviceRepository;
        this.faqRepository = faqRepository;
        this.auditEventService = auditEventService;
    }

    // =========================================================================
    // SITE SETTINGS
    // =========================================================================

    @Transactional(readOnly = true)
    @Cacheable(AppCacheNames.PUBLIC_SITE_SETTINGS)
    public PublicSiteSettingsResponse getPublicSiteSettings() {
        return settingsRepository.findTopByOrderByCreatedAtAsc()
                .map(PublicSiteSettingsResponse::fromEntity)
                .orElseGet(() -> PublicSiteSettingsResponse.fromEntity(null));
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_SITE_SETTINGS, allEntries = true)
    public PublicSiteSettingsResponse updateSiteSettings(UpdateSiteSettingsRequest request, User adminUser) {
        PublicSiteSettings settings = settingsRepository.findTopByOrderByCreatedAtAsc()
                .orElseGet(() -> {
                    PublicSiteSettings s = new PublicSiteSettings();
                    return settingsRepository.save(s);
                });

        settings.setSiteName(request.siteName().trim());
        if (request.tagline() != null) settings.setTagline(request.tagline().trim());
        if (request.supportEmail() != null) settings.setSupportEmail(request.supportEmail().trim());
        if (request.supportPhone() != null) settings.setSupportPhone(request.supportPhone().trim());
        if (request.contactAddress() != null) settings.setContactAddress(request.contactAddress().trim());
        if (request.facebookUrl() != null) settings.setFacebookUrl(request.facebookUrl().trim());
        if (request.instagramUrl() != null) settings.setInstagramUrl(request.instagramUrl().trim());
        if (request.linkedinUrl() != null) settings.setLinkedinUrl(request.linkedinUrl().trim());
        if (request.youtubeUrl() != null) settings.setYoutubeUrl(request.youtubeUrl().trim());
        if (request.xUrl() != null) settings.setXUrl(request.xUrl().trim());
        if (request.logoStorageKey() != null) settings.setLogoStorageKey(request.logoStorageKey().trim());
        if (request.faviconStorageKey() != null) settings.setFaviconStorageKey(request.faviconStorageKey().trim());
        if (request.defaultMetaTitle() != null) settings.setDefaultMetaTitle(request.defaultMetaTitle().trim());
        if (request.defaultMetaDescription() != null) settings.setDefaultMetaDescription(request.defaultMetaDescription().trim());
        if (request.copyrightText() != null) settings.setCopyrightText(request.copyrightText().trim());
        if (request.registrationEnabled() != null) settings.setRegistrationEnabled(request.registrationEnabled());
        if (request.publicPricingEnabled() != null) settings.setPublicPricingEnabled(request.publicPricingEnabled());
        settings.setUpdatedBy(adminUser);

        PublicSiteSettings saved = settingsRepository.save(settings);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.PUBLIC_CONTENT_UPDATED, "PublicSiteSettings", saved.getId(),
                "{\"updated\":\"site_settings\"}");

        return PublicSiteSettingsResponse.fromEntity(saved);
    }

    // =========================================================================
    // PUBLIC PAGES & SECTIONS (READ)
    // =========================================================================

    @Transactional(readOnly = true)
    @Cacheable(value = AppCacheNames.PUBLIC_PAGES, key = "#slug")
    public PublicPageResponse getPublishedPageBySlug(String slug) {
        PublicPage page = pageRepository.findBySlugWithSections(slug, PublicPageStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + slug));

        return PublicPageResponse.fromEntity(page, true);
    }

    @Transactional(readOnly = true)
    public List<PublicPageSummaryResponse> getPublishedNavigationPages() {
        return pageRepository.findByStatusAndShowInNavigationTrueOrderByNavigationOrderAsc(PublicPageStatus.PUBLISHED)
                .stream()
                .map(PublicPageSummaryResponse::fromEntity)
                .toList();
    }

    // =========================================================================
    // PAGES (ADMIN)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PublicPageSummaryResponse> getAllPagesForAdmin() {
        return pageRepository.findAllByOrderByNavigationOrderAsc()
                .stream()
                .map(PublicPageSummaryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicPageResponse getPageByIdForAdmin(UUID pageId) {
        PublicPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found with ID: " + pageId));
        return PublicPageResponse.fromEntity(page, false);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_PAGES, allEntries = true)
    public PublicPageResponse createPage(CreateOrUpdatePageRequest request, User adminUser) {
        String normalizedSlug = request.slug().trim().toLowerCase();
        if (pageRepository.existsBySlugIgnoreCase(normalizedSlug)) {
            throw new DuplicateResourceException("A page with slug '" + normalizedSlug + "' already exists.");
        }

        PublicPage page = new PublicPage();
        page.setType(request.type());
        page.setSlug(normalizedSlug);
        page.setTitle(request.title().trim());
        page.setSubtitle(request.subtitle() != null ? request.subtitle().trim() : null);
        page.setMetaTitle(request.metaTitle() != null ? request.metaTitle().trim() : null);
        page.setMetaDescription(request.metaDescription() != null ? request.metaDescription().trim() : null);
        page.setStatus(request.status() != null ? request.status() : PublicPageStatus.DRAFT);
        page.setShowInNavigation(request.showInNavigation() != null ? request.showInNavigation() : true);
        page.setNavigationOrder(request.navigationOrder() != null ? request.navigationOrder() : 0);
        if (page.getStatus() == PublicPageStatus.PUBLISHED) {
            page.setPublishedAt(OffsetDateTime.now());
        }
        page.setCreatedBy(adminUser);
        page.setUpdatedBy(adminUser);

        PublicPage saved = pageRepository.save(page);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.PUBLIC_CONTENT_UPDATED, "PublicPage", saved.getId(),
                "{\"created_page\":\"" + saved.getSlug() + "\"}");

        return PublicPageResponse.fromEntity(saved, false);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_PAGES, allEntries = true)
    public PublicPageResponse updatePage(UUID pageId, CreateOrUpdatePageRequest request, User adminUser) {
        PublicPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found with ID: " + pageId));

        String normalizedSlug = request.slug().trim().toLowerCase();
        if (!page.getSlug().equalsIgnoreCase(normalizedSlug) && pageRepository.existsBySlugIgnoreCase(normalizedSlug)) {
            throw new DuplicateResourceException("A page with slug '" + normalizedSlug + "' already exists.");
        }

        page.setType(request.type());
        page.setSlug(normalizedSlug);
        page.setTitle(request.title().trim());
        page.setSubtitle(request.subtitle() != null ? request.subtitle().trim() : null);
        page.setMetaTitle(request.metaTitle() != null ? request.metaTitle().trim() : null);
        page.setMetaDescription(request.metaDescription() != null ? request.metaDescription().trim() : null);

        if (request.status() != null && request.status() != page.getStatus()) {
            page.setStatus(request.status());
            if (request.status() == PublicPageStatus.PUBLISHED && page.getPublishedAt() == null) {
                page.setPublishedAt(OffsetDateTime.now());
            }
        }

        if (request.showInNavigation() != null) page.setShowInNavigation(request.showInNavigation());
        if (request.navigationOrder() != null) page.setNavigationOrder(request.navigationOrder());
        page.setUpdatedBy(adminUser);

        PublicPage saved = pageRepository.save(page);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.PUBLIC_CONTENT_UPDATED, "PublicPage", saved.getId(),
                "{\"updated_page\":\"" + saved.getSlug() + "\"}");

        return PublicPageResponse.fromEntity(saved, false);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_PAGES, allEntries = true)
    public void deletePage(UUID pageId, User adminUser) {
        PublicPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found with ID: " + pageId));

        pageRepository.delete(page);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.PUBLIC_CONTENT_UPDATED, "PublicPage", pageId,
                "{\"deleted_page\":\"" + page.getSlug() + "\"}");
    }

    // =========================================================================
    // SECTIONS (ADMIN)
    // =========================================================================

    @CacheEvict(value = AppCacheNames.PUBLIC_PAGES, allEntries = true)
    public PublicPageSectionResponse createSection(UUID pageId, CreateOrUpdateSectionRequest request, User adminUser) {
        PublicPage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found with ID: " + pageId));

        String sectionKey = request.sectionKey().trim().toLowerCase();
        if (sectionRepository.findByPageIdAndSectionKey(pageId, sectionKey).isPresent()) {
            throw new DuplicateResourceException("A section with key '" + sectionKey + "' already exists on this page.");
        }

        PublicPageSection section = new PublicPageSection();
        section.setPage(page);
        section.setType(request.type());
        section.setSectionKey(sectionKey);
        section.setEyebrow(request.eyebrow());
        section.setHeading(request.heading());
        section.setSubheading(request.subheading());
        section.setBody(request.body());
        section.setImageStorageKey(request.imageStorageKey());
        section.setPrimaryCtaLabel(request.primaryCtaLabel());
        section.setPrimaryCtaUrl(request.primaryCtaUrl());
        section.setSecondaryCtaLabel(request.secondaryCtaLabel());
        section.setSecondaryCtaUrl(request.secondaryCtaUrl());
        section.setConfigurationJson(request.configurationJson());
        section.setEnabled(request.enabled() != null ? request.enabled() : true);
        section.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : page.getSections().size());
        page.getSections().add(section);

        PublicPageSection saved = sectionRepository.save(section);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.PUBLIC_CONTENT_UPDATED, "PublicPageSection", saved.getId(),
                "{\"created_section\":\"" + saved.getSectionKey() + "\",\"page\":\"" + page.getSlug() + "\"}");

        return PublicPageSectionResponse.fromEntity(saved);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_PAGES, allEntries = true)
    public PublicPageSectionResponse updateSection(UUID pageId, UUID sectionId, CreateOrUpdateSectionRequest request, User adminUser) {
        PublicPageSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + sectionId));

        if (!section.getPage().getId().equals(pageId)) {
            throw new ResourceNotFoundException("Section does not belong to page: " + pageId);
        }

        String sectionKey = request.sectionKey().trim().toLowerCase();
        if (!section.getSectionKey().equalsIgnoreCase(sectionKey) &&
                sectionRepository.findByPageIdAndSectionKey(pageId, sectionKey).isPresent()) {
            throw new DuplicateResourceException("A section with key '" + sectionKey + "' already exists on this page.");
        }

        section.setType(request.type());
        section.setSectionKey(sectionKey);
        section.setEyebrow(request.eyebrow());
        section.setHeading(request.heading());
        section.setSubheading(request.subheading());
        section.setBody(request.body());
        section.setImageStorageKey(request.imageStorageKey());
        section.setPrimaryCtaLabel(request.primaryCtaLabel());
        section.setPrimaryCtaUrl(request.primaryCtaUrl());
        section.setSecondaryCtaLabel(request.secondaryCtaLabel());
        section.setSecondaryCtaUrl(request.secondaryCtaUrl());
        section.setConfigurationJson(request.configurationJson());
        if (request.enabled() != null) section.setEnabled(request.enabled());
        if (request.displayOrder() != null) section.setDisplayOrder(request.displayOrder());

        PublicPageSection saved = sectionRepository.save(section);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.PUBLIC_CONTENT_UPDATED, "PublicPageSection", saved.getId(),
                "{\"updated_section\":\"" + saved.getSectionKey() + "\"}");

        return PublicPageSectionResponse.fromEntity(saved);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_PAGES, allEntries = true)
    public void deleteSection(UUID pageId, UUID sectionId, User adminUser) {
        PublicPageSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + sectionId));

        if (!section.getPage().getId().equals(pageId)) {
            throw new ResourceNotFoundException("Section does not belong to page: " + pageId);
        }

        sectionRepository.delete(section);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.PUBLIC_CONTENT_UPDATED, "PublicPageSection", sectionId,
                "{\"deleted_section\":\"" + section.getSectionKey() + "\"}");
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_PAGES, allEntries = true)
    public void reorderSections(UUID pageId, ReorderSectionsRequest request, User adminUser) {
        List<UUID> order = request.sectionIds();
        for (int i = 0; i < order.size(); i++) {
            UUID id = order.get(i);
            int displayOrder = i;
            sectionRepository.findById(id).ifPresent(s -> {
                if (s.getPage().getId().equals(pageId)) {
                    s.setDisplayOrder(displayOrder);
                    sectionRepository.save(s);
                }
            });
        }
        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.PUBLIC_CONTENT_UPDATED, "PublicPage", pageId, "{\"reordered_sections\":true}");
    }

    // =========================================================================
    // SERVICE OFFERINGS
    // =========================================================================

    @Transactional(readOnly = true)
    @Cacheable(AppCacheNames.PUBLIC_SERVICES)
    public List<ServiceOfferingResponse> getActiveServices() {
        return serviceRepository.findByStatusOrderByDisplayOrderAsc(ServiceOfferingStatus.ACTIVE)
                .stream()
                .map(ServiceOfferingResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponse> getAllServicesForAdmin() {
        return serviceRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(ServiceOfferingResponse::fromEntity)
                .toList();
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_SERVICES, allEntries = true)
    public ServiceOfferingResponse createService(CreateOrUpdateServiceRequest request, User adminUser) {
        String code = request.code().trim().toUpperCase();
        if (serviceRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("A service with code '" + code + "' already exists.");
        }

        ServiceOffering service = new ServiceOffering();
        service.setCode(code);
        service.setName(request.name().trim());
        service.setShortDescription(request.shortDescription().trim());
        service.setDescription(request.description());
        service.setIconKey(request.iconKey());
        service.setFeatureListJson(request.featureListJson());
        service.setCtaLabel(request.ctaLabel());
        service.setCtaUrl(request.ctaUrl());
        service.setFeatured(request.featured() != null ? request.featured() : false);
        service.setStatus(request.status() != null ? request.status() : ServiceOfferingStatus.ACTIVE);
        service.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);

        ServiceOffering saved = serviceRepository.save(service);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.SERVICE_CREATED, "ServiceOffering", saved.getId(),
                "{\"service_code\":\"" + saved.getCode() + "\"}");

        return ServiceOfferingResponse.fromEntity(saved);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_SERVICES, allEntries = true)
    public ServiceOfferingResponse updateService(UUID id, CreateOrUpdateServiceRequest request, User adminUser) {
        ServiceOffering service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + id));

        String code = request.code().trim().toUpperCase();
        if (!service.getCode().equalsIgnoreCase(code) && serviceRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("A service with code '" + code + "' already exists.");
        }

        service.setCode(code);
        service.setName(request.name().trim());
        service.setShortDescription(request.shortDescription().trim());
        service.setDescription(request.description());
        service.setIconKey(request.iconKey());
        service.setFeatureListJson(request.featureListJson());
        service.setCtaLabel(request.ctaLabel());
        service.setCtaUrl(request.ctaUrl());
        if (request.featured() != null) service.setFeatured(request.featured());
        if (request.status() != null) service.setStatus(request.status());
        if (request.displayOrder() != null) service.setDisplayOrder(request.displayOrder());

        ServiceOffering saved = serviceRepository.save(service);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.SERVICE_UPDATED, "ServiceOffering", saved.getId(),
                "{\"service_code\":\"" + saved.getCode() + "\"}");

        return ServiceOfferingResponse.fromEntity(saved);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_SERVICES, allEntries = true)
    public void deleteService(UUID id, User adminUser) {
        ServiceOffering service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + id));

        serviceRepository.delete(service);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.PUBLIC_CONTENT_UPDATED, "ServiceOffering", id,
                "{\"deleted_service\":\"" + service.getCode() + "\"}");
    }

    // =========================================================================
    // FAQS
    // =========================================================================

    @Transactional(readOnly = true)
    @Cacheable(AppCacheNames.PUBLIC_FAQS)
    public List<FaqItemResponse> getPublishedFaqs() {
        return faqRepository.findByPublishedTrueOrderByDisplayOrderAsc()
                .stream()
                .map(FaqItemResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FaqItemResponse> getAllFaqsForAdmin() {
        return faqRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(FaqItemResponse::fromEntity)
                .toList();
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_FAQS, allEntries = true)
    public FaqItemResponse createFaq(CreateOrUpdateFaqRequest request, User adminUser) {
        FaqItem item = new FaqItem();
        item.setQuestion(request.question().trim());
        item.setAnswer(request.answer().trim());
        item.setCategory(request.category());
        item.setFeatured(request.featured() != null ? request.featured() : false);
        item.setPublished(request.published() != null ? request.published() : true);
        item.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);

        FaqItem saved = faqRepository.save(item);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.FAQ_CREATED, "FaqItem", saved.getId(), "{\"faq_created\":true}");

        return FaqItemResponse.fromEntity(saved);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_FAQS, allEntries = true)
    public FaqItemResponse updateFaq(UUID id, CreateOrUpdateFaqRequest request, User adminUser) {
        FaqItem item = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found with ID: " + id));

        item.setQuestion(request.question().trim());
        item.setAnswer(request.answer().trim());
        item.setCategory(request.category());
        if (request.featured() != null) item.setFeatured(request.featured());
        if (request.published() != null) item.setPublished(request.published());
        if (request.displayOrder() != null) item.setDisplayOrder(request.displayOrder());

        FaqItem saved = faqRepository.save(item);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.FAQ_UPDATED, "FaqItem", saved.getId(), "{\"faq_updated\":true}");

        return FaqItemResponse.fromEntity(saved);
    }

    @CacheEvict(value = AppCacheNames.PUBLIC_FAQS, allEntries = true)
    public void deleteFaq(UUID id, User adminUser) {
        FaqItem item = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found with ID: " + id));

        faqRepository.delete(item);

        auditEventService.record(adminUser != null ? adminUser.getId() : null, "ADMIN", null, null,
                AuditEventType.PUBLIC_CONTENT_UPDATED, "FaqItem", id, "{\"faq_deleted\":true}");
    }
}
