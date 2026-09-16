package com.researchassistant.publicsite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.admin.SystemRole;
import com.researchassistant.admin.SystemUserRole;
import com.researchassistant.admin.SystemUserRoleRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.publicsite.dto.*;
import com.researchassistant.publicsite.entity.*;
import com.researchassistant.publicsite.repository.*;
import com.researchassistant.publicsite.service.PublicContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Transactional
class AdminPublicSiteIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemUserRoleRepository roleRepository;

    @Autowired
    private PublicSiteSettingsRepository settingsRepository;

    @Autowired
    private PublicPageRepository pageRepository;

    @Autowired
    private ServiceOfferingRepository serviceRepository;

    @Autowired
    private FaqItemRepository faqRepository;

    @Autowired
    private ContactSubmissionRepository contactSubmissionRepository;

    @Autowired
    private ContactResponseRepository contactResponseRepository;

    @Autowired
    private PublicContentBootstrapSeeder bootstrapSeeder;

    @Autowired
    private PublicContentService contentService;

    @Autowired
    private com.researchassistant.security.jwt.JwtTokenService jwtTokenService;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String regularToken;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setEmail("admin-" + System.currentTimeMillis() + "@researchassistant.ai");
        adminUser.setPasswordHash("hash");
        adminUser.setFirstName("Platform");
        adminUser.setLastName("Admin");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser = userRepository.save(adminUser);

        SystemUserRole adminRole = new SystemUserRole();
        adminRole.setUser(adminUser);
        adminRole.setRole(SystemRole.SYSTEM_ADMIN);
        roleRepository.save(adminRole);

        regularUser = new User();
        regularUser.setEmail("user-" + System.currentTimeMillis() + "@researchassistant.ai");
        regularUser.setPasswordHash("hash");
        regularUser.setFirstName("Regular");
        regularUser.setLastName("Researcher");
        regularUser.setStatus(UserStatus.ACTIVE);
        regularUser = userRepository.save(regularUser);

        adminToken = jwtTokenService.issueAccessToken(adminUser).tokenValue();
        regularToken = jwtTokenService.issueAccessToken(regularUser).tokenValue();
    }

    @Test
    void normalUserCannotUpdatePublicSiteSettings() throws Exception {
        UpdateSiteSettingsRequest request = new UpdateSiteSettingsRequest(
                "Hacked Site Name", null, null, null, null, null, null, null, null, null, null, null, null, null, null, true, true
        );

        mockMvc.perform(put("/api/v1/admin/public-site/settings")
                        .header("Authorization", "Bearer " + regularToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void systemAdminCanUpdateSiteSettingsAndEvictsCache() throws Exception {
        UpdateSiteSettingsRequest request = new UpdateSiteSettingsRequest(
                "Updated Research Portal",
                "Advanced AI Research Workbench",
                "contact@researchportal.edu",
                "+233200000000",
                "Accra Ghana",
                null, null, null, null, null,
                null, null,
                "Updated Portal Meta",
                "Meta description updated",
                "© 2026 Updated. All rights reserved.",
                true,
                true
        );

        mockMvc.perform(put("/api/v1/admin/public-site/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteName").value("Updated Research Portal"))
                .andExpect(jsonPath("$.tagline").value("Advanced AI Research Workbench"));

        // Verify public projection also reflects the update
        mockMvc.perform(get("/api/v1/public/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteName").value("Updated Research Portal"));
    }

    @Test
    void systemAdminCanManagePagesAndSections() throws Exception {
        CreateOrUpdatePageRequest pageRequest = new CreateOrUpdatePageRequest(
                PublicPageType.CUSTOM,
                "methodology-guide",
                "Methodology Guide",
                "A comprehensive empirical design guide",
                "Methodology Guide Meta",
                "Empirical design meta description",
                PublicPageStatus.PUBLISHED,
                true,
                10
        );

        // Create page
        String pageResponseJson = mockMvc.perform(post("/api/v1/admin/public-site/pages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("methodology-guide"))
                .andReturn().getResponse().getContentAsString();

        PublicPageResponse createdPage = objectMapper.readValue(pageResponseJson, PublicPageResponse.class);

        // Add section
        CreateOrUpdateSectionRequest sectionRequest = new CreateOrUpdateSectionRequest(
                PublicSectionType.TEXT,
                "intro_section",
                "Methodology Principles",
                "Sampling & Validity",
                "Ensure construct validity and internal reliability",
                "Detailed body explanation for researchers.",
                null, null, null, null, null, null,
                true,
                1
        );

        mockMvc.perform(post("/api/v1/admin/public-site/pages/" + createdPage.id() + "/sections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sectionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sectionKey").value("intro_section"))
                .andExpect(jsonPath("$.heading").value("Sampling & Validity"));

        // Verify public page displays the section
        mockMvc.perform(get("/api/v1/public/pages/methodology-guide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].sectionKey").value("intro_section"));
    }

    @Test
    void systemAdminCanManageContactSubmissionsAndRespond() throws Exception {
        ContactSubmission submission = new ContactSubmission();
        submission.setReferenceCode("CNT-2026-999001");
        submission.setName("Dr. Kwesi Mensah");
        submission.setEmail("kwesi.mensah@ug.edu.gh");
        submission.setSubject("Institutional License Inquiry");
        submission.setMessage("Does the AI Research Assistant offer departmental licenses for postgraduate supervision?");
        submission.setStatus(ContactSubmissionStatus.NEW);
        submission.setSource(ContactSubmissionSource.PUBLIC_WEBSITE);
        submission.setSubmittedAt(OffsetDateTime.now());
        submission = contactSubmissionRepository.save(submission);

        // 1. Search submissions
        mockMvc.perform(get("/api/v1/admin/contact-submissions?search=Kwesi")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].referenceCode").value("CNT-2026-999001"));

        // 2. Get detail (transitions NEW -> READ)
        mockMvc.perform(get("/api/v1/admin/contact-submissions/" + submission.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"))
                .andExpect(jsonPath("$.firstReadAt").isNotEmpty());

        // 3. Post internal note response
        CreateContactReplyRequest noteReply = new CreateContactReplyRequest(
                "Internal note: Discussing institutional pricing with Dean tomorrow.",
                ContactResponseChannel.INTERNAL_NOTE
        );

        mockMvc.perform(post("/api/v1/admin/contact-submissions/" + submission.getId() + "/respond")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteReply)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("INTERNAL_NOTE"))
                .andExpect(jsonPath("$.status").value("SENT"));

        ContactSubmission updatedSub = contactSubmissionRepository.findById(submission.getId()).orElseThrow();
        assertThat(updatedSub.getStatus()).isEqualTo(ContactSubmissionStatus.IN_PROGRESS);

        // 4. Close submission
        UpdateContactStatusRequest closeRequest = new UpdateContactStatusRequest(ContactSubmissionStatus.CLOSED);
        mockMvc.perform(patch("/api/v1/admin/contact-submissions/" + submission.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }

    @Test
    void bootstrapSeederDoesNotOverwriteAdminEditsOnRestart() {
        // Find existing or create settings
        PublicSiteSettings settings = settingsRepository.findTopByOrderByCreatedAtAsc()
                .orElseGet(PublicSiteSettings::new);
        settings.setSiteName("Custom University Portal");
        settingsRepository.save(settings);

        // Re-run seeder as if application restarted
        bootstrapSeeder.run(null);

        // Verify custom site name was preserved
        PublicSiteSettings current = settingsRepository.findTopByOrderByCreatedAtAsc().orElseThrow();
        assertThat(current.getSiteName()).isEqualTo("Custom University Portal");
    }
}
