package com.researchassistant.identity.service;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.common.storage.ObjectStorageService;
import com.researchassistant.common.storage.StorageObjectCategory;
import com.researchassistant.common.storage.StorageObjectEntity;
import com.researchassistant.common.storage.StorageObjectMetadataService;
import com.researchassistant.common.storage.StorageObject;
import com.researchassistant.common.storage.StoredObject;
import com.researchassistant.identity.dto.ProfileImageDto;
import com.researchassistant.identity.dto.UserProfileResponse;
import com.researchassistant.identity.entity.ProfileImageStatus;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserProfileImage;
import com.researchassistant.identity.repository.UserProfileImageRepository;
import com.researchassistant.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class UserProfileService {

    private final UserProfileImageRepository profileImageRepository;
    private final ObjectStorageService objectStorageService;
    private final ProfileImageSecurityValidator validator;
    private final UserRepository userRepository;
    private final com.researchassistant.admin.SystemUserRoleRepository systemUserRoleRepository;
    private final StorageObjectMetadataService storageObjectMetadataService;

    public UserProfileService(
            UserProfileImageRepository profileImageRepository,
            ObjectStorageService objectStorageService,
            ProfileImageSecurityValidator validator,
            UserRepository userRepository,
            com.researchassistant.admin.SystemUserRoleRepository systemUserRoleRepository,
            StorageObjectMetadataService storageObjectMetadataService
    ) {
        this.profileImageRepository = profileImageRepository;
        this.objectStorageService = objectStorageService;
        this.validator = validator;
        this.userRepository = userRepository;
        this.systemUserRoleRepository = systemUserRoleRepository;
        this.storageObjectMetadataService = storageObjectMetadataService;
    }

    public record ProfileImageData(
            InputStream inputStream,
            String contentType,
            long sizeBytes,
            String checksumSha256
    ) {
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User user) {
        ProfileImageDto imageDto = profileImageRepository.findFirstByUserIdAndStatus(user.getId(), ProfileImageStatus.ACTIVE)
                .map(img -> new ProfileImageDto(
                        true,
                        "/api/v1/me/profile/image",
                        img.getUpdatedAt(),
                        img.getWidth(),
                        img.getHeight(),
                        img.getSizeBytes()
                ))
                .orElse(ProfileImageDto.empty());

        return buildResponse(user, imageDto);
    }

    @Transactional
    public UserProfileResponse updateProfile(User user, com.researchassistant.identity.dto.UpdateProfileRequest request) {
        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim().isEmpty() ? null : request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim().isEmpty() ? null : request.lastName().trim());
        }
        if (request.phoneNumber() != null) {
            if (request.phoneNumber().isBlank()) {
                user.setPhoneNumber(null);
            } else {
                String normalized = com.researchassistant.identity.util.PhoneNumberNormalizer.normalize(request.phoneNumber());
                user.setPhoneNumber(normalized);
            }
        }
        if (request.locale() != null && !request.locale().isBlank()) {
            user.setLocale(request.locale().trim());
        }

        User savedUser = userRepository.save(user);

        return getProfile(savedUser);
    }

    @Transactional
    public UserProfileResponse uploadProfileImage(User user, MultipartFile file) {
        ProfileImageSecurityValidator.ValidatedImageMetadata validated = validator.validate(file);

        // Generate controlled server-side storage key
        UUID imageId = UUID.randomUUID();
        String storedFilename = imageId + extension(validated.safeOriginalFilename());
        String storageKey = "users/" + user.getId() + "/general/profile/" + storedFilename;

        StorageObjectEntity pendingStorageObject = storageObjectMetadataService.createPending(
                user.getId(),
                null,
                null,
                StorageObjectCategory.PROFILE_IMAGE,
                storageKey,
                validated.safeOriginalFilename(),
                storedFilename,
                validated.normalizedContentType(),
                validated.bytes().length,
                null
        );

        StoredObject stored;
        StorageObjectEntity availableStorageObject;
        try {
            stored = objectStorageService.store(
                    storageKey,
                    new ByteArrayInputStream(validated.bytes()),
                    validated.normalizedContentType()
            );
            availableStorageObject = storageObjectMetadataService.markAvailable(
                    pendingStorageObject.getId(),
                    stored
            );
        } catch (RuntimeException exception) {
            storageObjectMetadataService.markFailed(
                    pendingStorageObject.getId(),
                    storageFailureCode(exception),
                    exception.getMessage()
            );
            throw exception;
        }

        // Mark any currently active image as REPLACED
        List<UserProfileImage> activeImages = profileImageRepository.findAllByUserIdAndStatus(
                user.getId(),
                ProfileImageStatus.ACTIVE
        );
        for (UserProfileImage active : activeImages) {
            active.setStatus(ProfileImageStatus.REPLACED);
            profileImageRepository.save(active);
        }
        profileImageRepository.flush();

        // Save new active image record
        UserProfileImage newImage = new UserProfileImage();
        newImage.setId(imageId);
        newImage.setUser(user);
        newImage.setStorageKey(stored.key());
        newImage.setStorageObject(availableStorageObject);
        newImage.setOriginalFilename(validated.safeOriginalFilename());
        newImage.setContentType(validated.normalizedContentType());
        newImage.setSizeBytes(stored.sizeBytes());
        newImage.setChecksumSha256(stored.checksumSha256());
        newImage.setWidth(validated.width());
        newImage.setHeight(validated.height());
        newImage.setStatus(ProfileImageStatus.ACTIVE);

        UserProfileImage saved = profileImageRepository.save(newImage);

        ProfileImageDto imageDto = new ProfileImageDto(
                true,
                "/api/v1/me/profile/image",
                saved.getUpdatedAt(),
                saved.getWidth(),
                saved.getHeight(),
                saved.getSizeBytes()
        );

        return buildResponse(user, imageDto);
    }

    @Transactional
    public UserProfileResponse deleteProfileImage(User user) {
        List<UserProfileImage> activeImages = profileImageRepository.findAllByUserIdAndStatus(
                user.getId(),
                ProfileImageStatus.ACTIVE
        );

        for (UserProfileImage active : activeImages) {
            active.setStatus(ProfileImageStatus.DELETED);
            profileImageRepository.save(active);
            try {
                objectStorageService.delete(active.getStorageKey());
                storageObjectMetadataService.markDeleted(active.getStorageObject());
            } catch (Exception ignored) {
            }
        }

        return buildResponse(user, ProfileImageDto.empty());
    }

    @Transactional(readOnly = true)
    public ProfileImageData loadProfileImage(UUID userId) {
        UserProfileImage activeImage = profileImageRepository.findFirstByUserIdAndStatus(
                userId,
                ProfileImageStatus.ACTIVE
        ).orElseThrow(() -> new ResourceNotFoundException("Profile image not found."));

        StorageObject storageObject = objectStorageService.open(activeImage.getStorageKey());

        return new ProfileImageData(
                storageObject.inputStream(),
                activeImage.getContentType(),
                storageObject.sizeBytes(),
                activeImage.getChecksumSha256()
        );
    }

    private UserProfileResponse buildResponse(User user, ProfileImageDto imageDto) {
        String displayName = (user.getFirstName() != null || user.getLastName() != null)
                ? String.format("%s %s",
                user.getFirstName() != null ? user.getFirstName() : "",
                user.getLastName() != null ? user.getLastName() : "").trim()
                : user.getEmail();

        if (displayName.isBlank()) {
            displayName = user.getEmail();
        }

        List<String> roles = systemUserRoleRepository.findAllByUserId(user.getId())
                .stream()
                .map(r -> r.getRole().name())
                .toList();

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                displayName,
                user.getPhoneNumber(),
                user.getLocale(),
                roles,
                imageDto,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String value = filename.substring(dot).toLowerCase(java.util.Locale.ROOT);
        return value.length() <= 10 && value.matches("\\.[a-z0-9]+") ? value : "";
    }

    private String storageFailureCode(RuntimeException exception) {
        if (exception instanceof com.researchassistant.common.storage.StorageException storageException) {
            return storageException.getErrorCode();
        }
        return exception.getClass().getSimpleName();
    }
}
