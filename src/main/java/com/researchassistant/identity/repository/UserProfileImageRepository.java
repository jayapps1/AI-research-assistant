package com.researchassistant.identity.repository;

import com.researchassistant.identity.entity.ProfileImageStatus;
import com.researchassistant.identity.entity.UserProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileImageRepository extends JpaRepository<UserProfileImage, UUID> {

    Optional<UserProfileImage> findFirstByUserIdAndStatus(UUID userId, ProfileImageStatus status);

    List<UserProfileImage> findAllByUserIdAndStatus(UUID userId, ProfileImageStatus status);

    List<UserProfileImage> findAllByUserId(UUID userId);
}
