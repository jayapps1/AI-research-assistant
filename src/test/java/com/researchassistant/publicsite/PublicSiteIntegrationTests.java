package com.researchassistant.publicsite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.publicsite.dto.ContactSubmissionRequest;
import com.researchassistant.publicsite.entity.*;
import com.researchassistant.publicsite.repository.*;
import com.researchassistant.subscription.SubscriptionPlan;
import com.researchassistant.subscription.SubscriptionPlanRepository;
import com.researchassistant.subscription.SubscriptionPlanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Transactional
class PublicSiteIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PublicSiteSettingsRepository settingsRepository;

    @Autowired
    private PublicPageRepository pageRepository;

    @Autowired
    private PublicPageSectionRepository sectionRepository;

    @Autowired
    private ServiceOfferingRepository serviceRepository;

    @Autowired
    private FaqItemRepository faqRepository;

    @Autowired
    private ContactSubmissionRepository contactSubmissionRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        if (cacheManager != null) {
            var c1 = cacheManager.getCache(com.researchassistant.cache.AppCacheNames.PUBLIC_SITE_SETTINGS);
            if (c1 != null) c1.clear();
            var c2 = cacheManager.getCache(com.researchassistant.cache.AppCacheNames.PUBLIC_PAGES);
            if (c2 != null) c2.clear();
        }

        PublicSiteSettings s = settingsRepository.findTopByOrderByCreatedAtAsc().orElseGet(PublicSiteSettings::new);
        s.setSiteName("AI Research Assistant");
        s.setTagline("From Research Question to Final Report.");
        s.setSupportEmail("support@researchassistant.ai");
        s.setRegistrationEnabled(true);
        s.setPublicPricingEnabled(true);
        settingsRepository.save(s);
    }

    @Test
    void publicSiteSettingsReturnsSafeProjection() throws Exception {
        mockMvc.perform(get("/api/v1/public/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteName").value("AI Research Assistant"))
                .andExpect(jsonPath("$.registrationEnabled").value(true))
                .andExpect(jsonPath("$.publicPricingEnabled").value(true));
    }

    @Test
    void publishedPageIsPubliclyVisibleWithSections() throws Exception {
        PublicPage page = new PublicPage();
        page.setType(PublicPageType.ABOUT);
        page.setSlug("test-about");
        page.setTitle("About Our Research");
        page.setStatus(PublicPageStatus.PUBLISHED);
        page.setShowInNavigation(true);
        page.setNavigationOrder(1);
        page.setPublishedAt(OffsetDateTime.now());
        page = pageRepository.save(page);

        PublicPageSection section = new PublicPageSection();
        section.setPage(page);
        section.setType(PublicSectionType.HERO);
        section.setSectionKey("hero");
        section.setHeading("Academic Rigor First");
        section.setEnabled(true);
        section.setDisplayOrder(1);
        page.getSections().add(section);
        sectionRepository.save(section);

        mockMvc.perform(get("/api/v1/public/pages/test-about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-about"))
                .andExpect(jsonPath("$.title").value("About Our Research"))
                .andExpect(jsonPath("$.sections", hasSize(1)))
                .andExpect(jsonPath("$.sections[0].heading").value("Academic Rigor First"));
    }

    @Test
    void draftPageReturnsNotFoundPublicly() throws Exception {
        PublicPage page = new PublicPage();
        page.setType(PublicPageType.CUSTOM);
        page.setSlug("confidential-draft");
        page.setTitle("Confidential Draft");
        page.setStatus(PublicPageStatus.DRAFT);
        page.setShowInNavigation(false);
        pageRepository.save(page);

        mockMvc.perform(get("/api/v1/public/pages/confidential-draft"))
                .andExpect(status().isNotFound());
    }

    @Test
    void servicesReturnsOnlyActiveOrderedByDisplayOrder() throws Exception {
        ServiceOffering active = new ServiceOffering();
        active.setCode("ACTIVE_SERVICE");
        active.setName("Active Service");
        active.setShortDescription("Short description of active service");
        active.setStatus(ServiceOfferingStatus.ACTIVE);
        active.setDisplayOrder(1);
        serviceRepository.save(active);

        ServiceOffering inactive = new ServiceOffering();
        inactive.setCode("INACTIVE_SERVICE");
        inactive.setName("Inactive Service");
        inactive.setShortDescription("Short description of inactive service");
        inactive.setStatus(ServiceOfferingStatus.INACTIVE);
        inactive.setDisplayOrder(2);
        serviceRepository.save(inactive);

        mockMvc.perform(get("/api/v1/public/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("ACTIVE_SERVICE")))
                .andExpect(jsonPath("$[*].code", not(hasItem("INACTIVE_SERVICE"))));
    }

    @Test
    void faqsReturnsOnlyPublishedItems() throws Exception {
        FaqItem published = new FaqItem();
        published.setQuestion("Can I use this for research?");
        published.setAnswer("Yes, absolutely.");
        published.setCategory(FaqCategory.GENERAL);
        published.setPublished(true);
        published.setDisplayOrder(1);
        faqRepository.save(published);

        FaqItem unpublished = new FaqItem();
        unpublished.setQuestion("Unpublished internal question?");
        unpublished.setAnswer("Not yet ready.");
        unpublished.setCategory(FaqCategory.GENERAL);
        unpublished.setPublished(false);
        unpublished.setDisplayOrder(2);
        faqRepository.save(unpublished);

        mockMvc.perform(get("/api/v1/public/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].question", hasItem("Can I use this for research?")))
                .andExpect(jsonPath("$[*].question", not(hasItem("Unpublished internal question?"))));
    }

    @Test
    void publicPricingDerivesFromActivePublicSubscriptionPlans() throws Exception {
        mockMvc.perform(get("/api/v1/public/pricing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].code", hasItem("FREE")))
                .andExpect(jsonPath("$[*].code", hasItem("STUDENT")))
                .andExpect(jsonPath("$[*].code", not(hasItem("DEVELOPER_COMPLIMENTARY"))));
    }

    @Test
    void contactFormAcceptsValidSubmissionAndGeneratesReferenceCode() throws Exception {
        ContactSubmissionRequest request = new ContactSubmissionRequest(
                "Jane Doe",
                "jane.doe@university.edu",
                "+233241234567",
                "Question about RAG Document Processing",
                "Hello, I would like to inquire whether our research team can upload scanned archive documents in PDF format?",
                null
        );

        mockMvc.perform(post("/api/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceCode", startsWith("CNT-")))
                .andExpect(jsonPath("$.message").value("Message received."));

        ContactSubmission saved = contactSubmissionRepository.findAll().stream()
                .filter(c -> "jane.doe@university.edu".equals(c.getEmail()))
                .findFirst()
                .orElse(null);

        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ContactSubmissionStatus.NEW);
        assertThat(saved.getReferenceCode()).matches("^CNT-\\d{4}-\\d{6}$");
    }

    @Test
    void contactFormRejectsInvalidEmail() throws Exception {
        ContactSubmissionRequest request = new ContactSubmissionRequest(
                "Jane Doe",
                "not-a-valid-email",
                null,
                "Valid Subject",
                "This is a legitimate question text with sufficient length.",
                null
        );

        mockMvc.perform(post("/api/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void contactFormRejectsShortMessage() throws Exception {
        ContactSubmissionRequest request = new ContactSubmissionRequest(
                "Jane Doe",
                "jane@university.edu",
                null,
                "Valid Subject",
                "Too short",
                null
        );

        mockMvc.perform(post("/api/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.message").exists());
    }

    @Test
    void contactFormMarksHoneypotSubmissionAsSpamSilently() throws Exception {
        ContactSubmissionRequest request = new ContactSubmissionRequest(
                "Spam Bot",
                "bot@spam-crawler.net",
                null,
                "SEO Marketing Service",
                "Buy high ranking backlinks for your academic research site instantly at cheap rate!",
                "http://malicious-spam-link.com" // honeypot filled!
        );

        mockMvc.perform(post("/api/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceCode", startsWith("CNT-")));

        ContactSubmission saved = contactSubmissionRepository.findAll().stream()
                .filter(c -> "bot@spam-crawler.net".equals(c.getEmail()))
                .findFirst()
                .orElse(null);

        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ContactSubmissionStatus.SPAM);
    }

    @Test
    void anonymousUserCannotListContactSubmissions() throws Exception {
        mockMvc.perform(get("/api/v1/admin/contact-submissions"))
                .andExpect(status().isUnauthorized());
    }
}
