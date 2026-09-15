package com.researchassistant.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SystemUserRoleRepository extends JpaRepository<SystemUserRole, UUID> {
    boolean existsByUserIdAndRole(UUID userId, SystemRole role);
}
