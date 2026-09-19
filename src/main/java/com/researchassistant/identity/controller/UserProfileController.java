package com.researchassistant.identity.controller;

import com.researchassistant.identity.dto.UserProfileResponse;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.identity.service.UserProfileService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1")
public class UserProfileController {

    private final AuthenticatedUserResolver userResolver;
    private final UserProfileService profileService;

    public UserProfileController(
            AuthenticatedUserResolver userResolver,
            UserProfileService profileService
    ) {
        this.userResolver = userResolver;
        this.profileService = profileService;
    }

    @GetMapping("/me/profile")
    public UserProfileResponse getMyProfile(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        return profileService.getProfile(user);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/me/profile")
    public UserProfileResponse updateMyProfile(
            Authentication authentication,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.researchassistant.identity.dto.UpdateProfileRequest request
    ) {
        User user = userResolver.requireActiveUser(authentication);
        return profileService.updateProfile(user, request);
    }

    @PostMapping(value = "/me/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileResponse uploadProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        User user = userResolver.requireActiveUser(authentication);
        return profileService.uploadProfileImage(user, file);
    }

    @DeleteMapping("/me/profile/image")
    public UserProfileResponse deleteProfileImage(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        return profileService.deleteProfileImage(user);
    }

    @GetMapping("/me/profile/image")
    public ResponseEntity<InputStreamResource> getMyProfileImage(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        return streamProfileImage(user.getId());
    }

    @GetMapping("/users/{userId}/avatar")
    public ResponseEntity<InputStreamResource> getUserAvatar(
            @PathVariable UUID userId
    ) {
        return streamProfileImage(userId);
    }

    private ResponseEntity<InputStreamResource> streamProfileImage(UUID userId) {
        UserProfileService.ProfileImageData data = profileService.loadProfileImage(userId);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(data.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(data.sizeBytes())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new InputStreamResource(data.inputStream()));
    }
}
