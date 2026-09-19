package com.researchassistant.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.identity.entity.ProfileImageStatus;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserProfileImageRepository;
import com.researchassistant.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Transactional
class UserProfileIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileImageRepository profileImageRepository;

    @Autowired
    private com.researchassistant.security.jwt.JwtTokenService jwtTokenService;

    private User testUser;
    private String testToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("profile.test." + System.currentTimeMillis() + "@example.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setFirstName("Profile");
        testUser.setLastName("Tester");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCreatedAt(OffsetDateTime.now());
        testUser.setUpdatedAt(OffsetDateTime.now());
        testUser = userRepository.save(testUser);

        testToken = jwtTokenService.issueAccessToken(testUser).tokenValue();
    }

    private byte[] createTestImageBytes(String format, int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.CYAN);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }

    @Test
    void getProfile_ReturnsCurrentProfileWithoutAvatarInitially() throws Exception {
        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testUser.getId().toString())))
                .andExpect(jsonPath("$.email", is(testUser.getEmail())))
                .andExpect(jsonPath("$.firstName", is("Profile")))
                .andExpect(jsonPath("$.lastName", is("Tester")))
                .andExpect(jsonPath("$.profileImage.available", is(false)));
    }

    @Test
    void uploadProfileImage_ValidPng_SavesActiveImageAndAllowsRetrieval() throws Exception {
        byte[] pngBytes = createTestImageBytes("png", 100, 100);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                pngBytes
        );

        mockMvc.perform(multipart("/api/v1/me/profile/image")
                        .file(file)
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testUser.getId().toString())))
                .andExpect(jsonPath("$.profileImage.available", is(true)))
                .andExpect(jsonPath("$.profileImage.url", notNullValue()))
                .andExpect(jsonPath("$.profileImage.width", is(100)))
                .andExpect(jsonPath("$.profileImage.height", is(100)));

        // Retrieve raw avatar (publicly accessible)
        mockMvc.perform(get("/api/v1/users/" + testUser.getId() + "/avatar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().exists("Cache-Control"))
                .andExpect(content().bytes(pngBytes));

        // Also test /me/profile/image endpoint
        mockMvc.perform(get("/api/v1/me/profile/image")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(pngBytes));
    }

    @Test
    void uploadProfileImage_ReplacingImage_MarksPreviousReplaced() throws Exception {
        byte[] firstBytes = createTestImageBytes("jpg", 80, 80);
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "first.jpg",
                "image/jpeg",
                firstBytes
        );

        mockMvc.perform(multipart("/api/v1/me/profile/image")
                        .file(file1)
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk());

        byte[] secondBytes = createTestImageBytes("png", 120, 120);
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "second.png",
                "image/png",
                secondBytes
        );

        mockMvc.perform(multipart("/api/v1/me/profile/image")
                        .file(file2)
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImage.available", is(true)))
                .andExpect(jsonPath("$.profileImage.width", is(120)))
                .andExpect(jsonPath("$.profileImage.height", is(120)));

        // Check DB state
        var userImages = profileImageRepository.findAllByUserId(testUser.getId());
        assertThat(userImages).hasSize(2);
        assertThat(userImages).anyMatch(img -> img.getStatus() == ProfileImageStatus.ACTIVE);
        assertThat(userImages).anyMatch(img -> img.getStatus() == ProfileImageStatus.REPLACED);
    }

    @Test
    void uploadProfileImage_CorruptOrMismatchedMagicBytes_RejectsWithBadRequest() throws Exception {
        byte[] badBytes = "not a real image at all".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file",
                "fake.jpg",
                "image/jpeg",
                badBytes
        );

        mockMvc.perform(multipart("/api/v1/me/profile/image")
                        .file(fakeFile)
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadProfileImage_DisallowedContentType_RejectsWithBadRequest() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "script.sh",
                "application/x-sh",
                "#!/bin/bash\necho hello".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/me/profile/image")
                        .file(textFile)
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProfileImage_RemovesActiveImage_Returns404ForRawAvatar() throws Exception {
        byte[] pngBytes = createTestImageBytes("png", 64, 64);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                pngBytes
        );

        mockMvc.perform(multipart("/api/v1/me/profile/image")
                        .file(file)
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk());

        // Delete active image
        mockMvc.perform(delete("/api/v1/me/profile/image")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImage.available", is(false)));

        // Raw avatar now 404s
        mockMvc.perform(get("/api/v1/users/" + testUser.getId() + "/avatar"))
                .andExpect(status().isNotFound());
    }
}
