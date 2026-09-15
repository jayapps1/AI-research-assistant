package com.researchassistant.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {
    List<UserDevice> findAllByUserId(UUID userId);
    Optional<UserDevice> findByIdAndUserId(UUID id, UUID userId);
    Optional<UserDevice> findByPushToken(String pushToken);
}
