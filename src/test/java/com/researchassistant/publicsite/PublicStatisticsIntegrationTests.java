package com.researchassistant.publicsite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.admin.SystemRole;
import com.researchassistant.admin.SystemUserRole;
import com.researchassistant.admin.SystemUserRoleRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.publicsite.dto.CreatePublicStatisticRequest;
import com.researchassistant.publicsite.dto.UpdatePublicStatisticRequest;
import com.researchassistant.publicsite.entity.PublicStatistic;
import com.researchassistant.publicsite.entity.PublicStatisticValueSource;
import com.researchassistant.publicsite.entity.PublicSystemMetric;
import com.researchassistant.publicsite.repository.PublicStatisticRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Transactional
class PublicStatisticsIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemUserRoleRepository roleRepository;

    @Autowired
    private PublicStatisticRepository statisticRepository;

    @Autowired
    private com.researchassistant.security.jwt.JwtTokenService jwtTokenService;

    private User adminUser;
    private User normalUser;
    private String adminToken;
    private String normalToken;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin.stats." + System.currentTimeMillis() + "@example.com");
        adminUser.setPasswordHash("hashedpassword");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setCreatedAt(OffsetDateTime.now());
        adminUser.setUpdatedAt(OffsetDateTime.now());
        adminUser = userRepository.save(adminUser);

        SystemUserRole adminRole = new SystemUserRole();
        adminRole.setUser(adminUser);
        adminRole.setRole(SystemRole.SYSTEM_ADMIN);
        adminRole.setCreatedAt(OffsetDateTime.now());
        roleRepository.save(adminRole);

        normalUser = new User();
        normalUser.setId(UUID.randomUUID());
        normalUser.setEmail("normal.user." + System.currentTimeMillis() + "@example.com");
        normalUser.setPasswordHash("hashedpassword");
        normalUser.setFirstName("Normal");
        normalUser.setLastName("User");
        normalUser.setStatus(UserStatus.ACTIVE);
        normalUser.setCreatedAt(OffsetDateTime.now());
        normalUser.setUpdatedAt(OffsetDateTime.now());
        normalUser = userRepository.save(normalUser);

        adminToken = jwtTokenService.issueAccessToken(adminUser).tokenValue();
        normalToken = jwtTokenService.issueAccessToken(normalUser).tokenValue();
    }

    @Test
    void getPublicStatistics_ReturnsOnlyEnabledStatisticsOrdered() throws Exception {
        statisticRepository.deleteAll();

        PublicStatistic stat1 = new PublicStatistic();
        stat1.setCode("active-users");
        stat1.setLabel("Active Researchers");
        stat1.setValueSource(PublicStatisticValueSource.SYSTEM_DERIVED);
        stat1.setSystemMetric(PublicSystemMetric.TOTAL_ACTIVE_USERS);
        stat1.setEnabled(true);
        stat1.setDisplayOrder(1);
        stat1.setSuffix("+");
        statisticRepository.save(stat1);

        PublicStatistic stat2 = new PublicStatistic();
        stat2.setCode("satisfaction");
        stat2.setLabel("Researcher Satisfaction");
        stat2.setValueSource(PublicStatisticValueSource.MANUAL);
        stat2.setManualValue("99.4%");
        stat2.setEnabled(true);
        stat2.setDisplayOrder(2);
        statisticRepository.save(stat2);

        PublicStatistic disabledStat = new PublicStatistic();
        disabledStat.setCode("disabled-metric");
        disabledStat.setLabel("Disabled Counter");
        disabledStat.setValueSource(PublicStatisticValueSource.MANUAL);
        disabledStat.setManualValue("0");
        disabledStat.setEnabled(false);
        disabledStat.setDisplayOrder(0);
        statisticRepository.save(disabledStat);

        mockMvc.perform(get("/api/v1/public/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code", is("active-users")))
                .andExpect(jsonPath("$[0].label", is("Active Researchers")))
                .andExpect(jsonPath("$[0].suffix", is("+")))
                .andExpect(jsonPath("$[1].code", is("satisfaction")))
                .andExpect(jsonPath("$[1].value", is("99.4%")));
    }

    @Test
    void adminStatistics_CrudAndPreviewLifecycle() throws Exception {
        // 1. Preview Metric
        mockMvc.perform(get("/api/v1/admin/public-site/statistics/preview-metric")
                        .param("metric", "TOTAL_ACTIVE_USERS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric", is("TOTAL_ACTIVE_USERS")))
                .andExpect(jsonPath("$.rawValue", greaterThanOrEqualTo(2)));

        // 2. Create Statistic
        CreatePublicStatisticRequest createReq = new CreatePublicStatisticRequest(
                "custom-stat-" + System.currentTimeMillis(),
                "Custom Empirical Studies",
                "Studies completed using verified methodology",
                PublicStatisticValueSource.SYSTEM_DERIVED,
                null,
                PublicSystemMetric.TOTAL_RESEARCH_PROJECTS,
                ">",
                " verified",
                "folder-kanban",
                true,
                true,
                1
        );

        String createJson = objectMapper.writeValueAsString(createReq);
        String responseBody = mockMvc.perform(post("/api/v1/admin/public-site/statistics")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is(createReq.code())))
                .andExpect(jsonPath("$.label", is(createReq.label())))
                .andExpect(jsonPath("$.featured", is(true)))
                .andReturn().getResponse().getContentAsString();

        UUID createdId = UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());

        // 3. Update Statistic
        UpdatePublicStatisticRequest updateReq = new UpdatePublicStatisticRequest(
                "Updated Empirical Studies",
                "Updated description",
                PublicStatisticValueSource.MANUAL,
                "5,200",
                null,
                null,
                "+",
                "chart",
                true,
                false,
                2
        );

        mockMvc.perform(put("/api/v1/admin/public-site/statistics/" + createdId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label", is("Updated Empirical Studies")))
                .andExpect(jsonPath("$.manualValue", is("5,200")))
                .andExpect(jsonPath("$.resolvedValue", is("5,200")))
                .andExpect(jsonPath("$.featured", is(false)));

        // 4. Delete Statistic
        mockMvc.perform(delete("/api/v1/admin/public-site/statistics/" + createdId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(statisticRepository.existsById(createdId)).isFalse();
    }

    @Test
    void adminStatistics_NonAdminUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/public-site/statistics")
                        .header("Authorization", "Bearer " + normalToken))
                .andExpect(status().isForbidden());
    }
}
