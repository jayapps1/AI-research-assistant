package com.researchassistant.publicsite.controller;

import com.researchassistant.admin.SystemAdminAuthorizationService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.publicsite.dto.*;
import com.researchassistant.publicsite.service.PublicContentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/public-site")
public class AdminPublicSiteController {

    private final AuthenticatedUserResolver userResolver;
    private final SystemAdminAuthorizationService adminAuthorizationService;
    private final PublicContentService contentService;

    public AdminPublicSiteController(AuthenticatedUserResolver userResolver,
                                     SystemAdminAuthorizationService adminAuthorizationService,
                                     PublicContentService contentService) {
        this.userResolver = userResolver;
        this.adminAuthorizationService = adminAuthorizationService;
        this.contentService = contentService;
    }

    private User requireAdmin(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        adminAuthorizationService.requireSystemAdmin(user.getId());
        return user;
    }

    // =========================================================================
    // SITE SETTINGS
    // =========================================================================

    @GetMapping("/settings")
    public PublicSiteSettingsResponse getSettings(Authentication authentication) {
        requireAdmin(authentication);
        return contentService.getPublicSiteSettings();
    }

    @PutMapping("/settings")
    public PublicSiteSettingsResponse updateSettings(
            @Valid @RequestBody UpdateSiteSettingsRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contentService.updateSiteSettings(request, admin);
    }

    // =========================================================================
    // PAGES
    // =========================================================================

    @GetMapping("/pages")
    public List<PublicPageSummaryResponse> getAllPages(Authentication authentication) {
        requireAdmin(authentication);
        return contentService.getAllPagesForAdmin();
    }

    @GetMapping("/pages/{id}")
    public PublicPageResponse getPageById(@PathVariable UUID id, Authentication authentication) {
        requireAdmin(authentication);
        return contentService.getPageByIdForAdmin(id);
    }

    @PostMapping("/pages")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicPageResponse createPage(
            @Valid @RequestBody CreateOrUpdatePageRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contentService.createPage(request, admin);
    }

    @PutMapping("/pages/{id}")
    public PublicPageResponse updatePage(
            @PathVariable UUID id,
            @Valid @RequestBody CreateOrUpdatePageRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contentService.updatePage(id, request, admin);
    }

    @DeleteMapping("/pages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePage(@PathVariable UUID id, Authentication authentication) {
        User admin = requireAdmin(authentication);
        contentService.deletePage(id, admin);
    }

    // =========================================================================
    // SECTIONS
    // =========================================================================

    @PostMapping("/pages/{pageId}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicPageSectionResponse createSection(
            @PathVariable UUID pageId,
            @Valid @RequestBody CreateOrUpdateSectionRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contentService.createSection(pageId, request, admin);
    }

    @PutMapping("/pages/{pageId}/sections/{sectionId}")
    public PublicPageSectionResponse updateSection(
            @PathVariable UUID pageId,
            @PathVariable UUID sectionId,
            @Valid @RequestBody CreateOrUpdateSectionRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contentService.updateSection(pageId, sectionId, request, admin);
    }

    @DeleteMapping("/pages/{pageId}/sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(
            @PathVariable UUID pageId,
            @PathVariable UUID sectionId,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        contentService.deleteSection(pageId, sectionId, admin);
    }

    @PutMapping("/pages/{pageId}/sections/reorder")
    public Map<String, Object> reorderSections(
            @PathVariable UUID pageId,
            @Valid @RequestBody ReorderSectionsRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        contentService.reorderSections(pageId, request, admin);
        return Map.of("status", "SUCCESS", "message", "Sections reordered.");
    }

    // =========================================================================
    // SERVICES
    // =========================================================================

    @GetMapping("/services")
    public List<ServiceOfferingResponse> getAllServices(Authentication authentication) {
        requireAdmin(authentication);
        return contentService.getAllServicesForAdmin();
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceOfferingResponse createService(
            @Valid @RequestBody CreateOrUpdateServiceRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contentService.createService(request, admin);
    }

    @PutMapping("/services/{id}")
    public ServiceOfferingResponse updateService(
            @PathVariable UUID id,
            @Valid @RequestBody CreateOrUpdateServiceRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contentService.updateService(id, request, admin);
    }

    @DeleteMapping("/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable UUID id, Authentication authentication) {
        User admin = requireAdmin(authentication);
        contentService.deleteService(id, admin);
    }

    // =========================================================================
    // FAQS
    // =========================================================================

    @GetMapping("/faqs")
    public List<FaqItemResponse> getAllFaqs(Authentication authentication) {
        requireAdmin(authentication);
        return contentService.getAllFaqsForAdmin();
    }

    @PostMapping("/faqs")
    @ResponseStatus(HttpStatus.CREATED)
    public FaqItemResponse createFaq(
            @Valid @RequestBody CreateOrUpdateFaqRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contentService.createFaq(request, admin);
    }

    @PutMapping("/faqs/{id}")
    public FaqItemResponse updateFaq(
            @PathVariable UUID id,
            @Valid @RequestBody CreateOrUpdateFaqRequest request,
            Authentication authentication) {
        User admin = requireAdmin(authentication);
        return contentService.updateFaq(id, request, admin);
    }

    @DeleteMapping("/faqs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFaq(@PathVariable UUID id, Authentication authentication) {
        User admin = requireAdmin(authentication);
        contentService.deleteFaq(id, admin);
    }
}
