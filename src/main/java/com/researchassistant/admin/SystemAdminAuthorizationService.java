package com.researchassistant.admin;

import com.researchassistant.workspace.exception.WorkspaceAccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SystemAdminAuthorizationService {
    private final SystemUserRoleRepository roleRepository;

    public SystemAdminAuthorizationService(SystemUserRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void requireSystemAdmin(UUID userId) {
        if (!roleRepository.existsByUserIdAndRole(userId, SystemRole.SYSTEM_ADMIN)) {
            throw new WorkspaceAccessDeniedException("System administrator access is required.");
        }
    }
}
