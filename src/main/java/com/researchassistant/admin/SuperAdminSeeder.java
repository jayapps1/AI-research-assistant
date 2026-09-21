package com.researchassistant.admin;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.audit.AuditEventType;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class SuperAdminSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final SuperAdminSeedProperties properties;
    private final UserRepository userRepository;
    private final SystemUserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final com.researchassistant.workspace.service.PersonalWorkspaceService personalWorkspaceService;

    @org.springframework.beans.factory.annotation.Autowired
    public SuperAdminSeeder(SuperAdminSeedProperties properties, UserRepository userRepository,
                            SystemUserRoleRepository roleRepository, PasswordEncoder passwordEncoder,
                            AuditEventService auditEventService,
                            com.researchassistant.workspace.service.PersonalWorkspaceService personalWorkspaceService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
        this.personalWorkspaceService = personalWorkspaceService;
    }

    public SuperAdminSeeder(SuperAdminSeedProperties properties, UserRepository userRepository,
                            SystemUserRoleRepository roleRepository, PasswordEncoder passwordEncoder,
                            AuditEventService auditEventService) {
        this(properties, userRepository, roleRepository, passwordEncoder, auditEventService, null);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }

        String email = normalizeEmail(properties.email());
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean created = false;

        if (user == null) {
            if (properties.password() == null || properties.password().isBlank()) {
                throw new IllegalStateException("SUPERADMIN_PASSWORD is required when creating the bootstrap system administrator.");
            }
            user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(properties.password()));
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerified(true);
            user.setLocale("en");
            user.setPhoneNumber(com.researchassistant.identity.util.PhoneNumberNormalizer.normalize(properties.phone()));
            user.setAuthenticationMethod(com.researchassistant.identity.entity.AuthenticationMethod.PASSWORD_OR_TOTP);
            user = userRepository.save(user);
            created = true;
        } else {
            boolean modified = false;
            if (user.getStatus() != UserStatus.ACTIVE) {
                user.setStatus(UserStatus.ACTIVE);
                modified = true;
            }
            if (user.getAuthenticationMethod() != com.researchassistant.identity.entity.AuthenticationMethod.PASSWORD_OR_TOTP) {
                user.setAuthenticationMethod(com.researchassistant.identity.entity.AuthenticationMethod.PASSWORD_OR_TOTP);
                modified = true;
            }
            if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
                if (properties.phone() != null && !properties.phone().isBlank()) {
                    user.setPhoneNumber(com.researchassistant.identity.util.PhoneNumberNormalizer.normalize(properties.phone()));
                    modified = true;
                }
            } else if (!user.getPhoneNumber().startsWith("+")) {
                try {
                    String normalized = com.researchassistant.identity.util.PhoneNumberNormalizer.normalize(user.getPhoneNumber());
                    if (!normalized.equals(user.getPhoneNumber())) {
                        user.setPhoneNumber(normalized);
                        modified = true;
                    }
                } catch (Exception ex) {
                    log.warn("Could not normalize existing admin phone {}: {}", user.getPhoneNumber(), ex.getMessage());
                }
            }
            if (modified) {
                userRepository.save(user);
            }
        }

        if (!roleRepository.existsByUserIdAndRole(user.getId(), SystemRole.SYSTEM_ADMIN)) {
            SystemUserRole role = new SystemUserRole();
            role.setUser(user);
            role.setRole(SystemRole.SYSTEM_ADMIN);
            roleRepository.save(role);
        }

        if (personalWorkspaceService != null) {
            personalWorkspaceService.ensurePersonalWorkspace(user);
        }

        auditEventService.record(null, "SYSTEM", null, null,
                AuditEventType.SYSTEM_ADMIN_BOOTSTRAPPED, "User", user.getId(),
                created ? "{\"created\":true}" : "{\"created\":false}");
        log.info("System administrator bootstrap account ensured.");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
