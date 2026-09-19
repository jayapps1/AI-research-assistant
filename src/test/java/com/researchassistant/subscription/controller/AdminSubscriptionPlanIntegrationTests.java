package com.researchassistant.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.IntegrationTestSupport;
import com.researchassistant.admin.SystemRole;
import com.researchassistant.admin.SystemUserRole;
import com.researchassistant.admin.SystemUserRoleRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.subscription.BillingInterval;
import com.researchassistant.subscription.LimitUnit;
import com.researchassistant.subscription.PlanFeature;
import com.researchassistant.subscription.SubscriptionPlanRepository;
import com.researchassistant.subscription.SubscriptionPlanStatus;
import com.researchassistant.subscription.dto.AdminPlanDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the admin subscription-plan management API.
 *
 * <p>Authentication is performed by generating real HMAC-SHA256 JWTs using
 * the same secret configured in the test {@code @SpringBootTest} properties.
 * This exercises the full security filter chain without mocks.</p>
 */
@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Transactional
class AdminSubscriptionPlanIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemUserRoleRepository roleRepository;

    @Autowired
    private SubscriptionPlanRepository planRepository;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        adminUser = new User();
        adminUser.setEmail("admin-plans-" + unique + "@researchassistant.com");
        adminUser.setPasswordHash("hashedpassword");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser = userRepository.save(adminUser);

        SystemUserRole adminRole = new SystemUserRole();
        adminRole.setUser(adminUser);
        adminRole.setRole(SystemRole.SYSTEM_ADMIN);
        roleRepository.save(adminRole);

        regularUser = new User();
        regularUser.setEmail("regular-user-" + unique + "@researchassistant.com");
        regularUser.setPasswordHash("hashedpassword");
        regularUser.setFirstName("Regular");
        regularUser.setLastName("Researcher");
        regularUser.setStatus(UserStatus.ACTIVE);
        regularUser = userRepository.save(regularUser);
    }

    @Test
    void nonAdminCannotAccessAdminPlans() throws Exception {
        mockMvc.perform(get("/api/v1/admin/subscription-plans")
                        .header("Authorization", bearerToken(regularUser.getId(), regularUser.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/subscription-plans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListPlans() throws Exception {
        mockMvc.perform(get("/api/v1/admin/subscription-plans")
                        .header("Authorization", bearerToken(adminUser.getId(), adminUser.getEmail())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void adminCanCreateProPlanAndManageLifecycle() throws Exception {
        String token = bearerToken(adminUser.getId(), adminUser.getEmail());
        String uniqueCode = "PRO_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        CreateSubscriptionPlanRequest createRequest = new CreateSubscriptionPlanRequest(
                uniqueCode,
                "Pro Researcher Plan",
                "Advanced AI synthesis and qualitative features",
                SubscriptionPlanStatus.ACTIVE,
                BillingInterval.MONTHLY,
                BigDecimal.valueOf(50.00),
                BigDecimal.valueOf(500.00),
                "GHS",
                true,
                true,
                2
        );

        // 1. Create plan
        String responseContent = mockMvc.perform(post("/api/v1/admin/subscription-plans")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is(uniqueCode)))
                .andExpect(jsonPath("$.name", is("Pro Researcher Plan")))
                .andExpect(jsonPath("$.price", is(50.00)))
                .andExpect(jsonPath("$.featured", is(true)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andReturn().getResponse().getContentAsString();

        AdminSubscriptionPlanResponse created = objectMapper.readValue(responseContent, AdminSubscriptionPlanResponse.class);
        UUID planId = created.id();

        // 2. Update plan
        UpdateSubscriptionPlanRequest updateRequest = new UpdateSubscriptionPlanRequest(
                "Pro Researcher Plus",
                "Updated description",
                null,
                null,
                BigDecimal.valueOf(65.00),
                BigDecimal.valueOf(650.00),
                "GHS",
                true,
                false,
                3
        );

        mockMvc.perform(patch("/api/v1/admin/subscription-plans/{planId}", planId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Pro Researcher Plus")))
                .andExpect(jsonPath("$.price", is(65.00)))
                .andExpect(jsonPath("$.featured", is(false)));

        // 3. Deactivate plan
        mockMvc.perform(post("/api/v1/admin/subscription-plans/{planId}/deactivate", planId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INACTIVE")));

        // 4. Reactivate plan
        mockMvc.perform(post("/api/v1/admin/subscription-plans/{planId}/activate", planId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // 5. Manage entitlements
        List<AdminPlanEntitlementDto> entitlements = List.of(
                new AdminPlanEntitlementDto(PlanFeature.PROJECT_CREATION, true, LimitMode.LIMITED, 10L, LimitUnit.PROJECTS, "10 projects"),
                new AdminPlanEntitlementDto(PlanFeature.STORAGE, true, LimitMode.LIMITED, 5368709120L, LimitUnit.BYTES, "5 GB"),
                new AdminPlanEntitlementDto(PlanFeature.QUALITATIVE_AI, true, LimitMode.UNLIMITED, null, LimitUnit.NONE, "Unlimited")
        );
        UpdateEntitlementsRequest entRequest = new UpdateEntitlementsRequest(entitlements);

        mockMvc.perform(put("/api/v1/admin/subscription-plans/{planId}/entitlements", planId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // 6. Read entitlements back
        mockMvc.perform(get("/api/v1/admin/subscription-plans/{planId}/entitlements", planId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(PlanFeature.values().length)))
                .andExpect(jsonPath("$[?(@.feature == 'PROJECT_CREATION')].limitValue", contains(10)))
                .andExpect(jsonPath("$[?(@.feature == 'STORAGE')].limitValue", contains(5368709120L)));
    }

    @Test
    void adminCanAccessPaymentsDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .header("Authorization", bearerToken(adminUser.getId(), adminUser.getEmail()))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }
}
