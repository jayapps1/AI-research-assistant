package com.researchassistant.admin;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuperAdminSeederTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SystemUserRoleRepository roleRepository =
            mock(SystemUserRoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuditEventService auditEventService =
            mock(AuditEventService.class);

    @Test
    void disabledSeederCreatesNothing() throws Exception {
        SuperAdminSeeder seeder = seeder(false, "", "seed-password");

        seeder.run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any());
        verify(roleRepository, never()).save(any());
    }

    @Test
    void enabledSeederCreatesMissingSuperadmin() throws Exception {
        when(userRepository.findByEmailIgnoreCase("nanagyachie@gmail.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("seed-password"))
                .thenReturn("{bcrypt}encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        seeder(true, " Nanagyachie@GMAIL.com ", "seed-password")
                .run(new DefaultApplicationArguments());

        verify(userRepository).save(any(User.class));
        verify(roleRepository).save(any(SystemUserRole.class));
        verify(passwordEncoder).encode("seed-password");
    }

    @Test
    void createdUserIsNormalizedActiveEncodedAndAssignedPhone() throws Exception {
        when(userRepository.findByEmailIgnoreCase("nanagyachie@gmail.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("seed-password"))
                .thenReturn("{bcrypt}encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        seeder(true, " Nanagyachie@GMAIL.com ", "seed-password")
                .run(new DefaultApplicationArguments());

        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getEmail().equals("nanagyachie@gmail.com")
                        && user.getStatus() == UserStatus.ACTIVE
                        && user.isEmailVerified()
                        && "+233542011738".equals(user.getPhoneNumber())
                        && "{bcrypt}encoded".equals(user.getPasswordHash())
                        && !"seed-password".equals(user.getPasswordHash())
        ));
        verify(roleRepository).save(org.mockito.ArgumentMatchers.argThat(role ->
                role.getRole() == SystemRole.SYSTEM_ADMIN
        ));
    }

    @Test
    void restartDoesNotCreateDuplicateOrOverwritePassword() throws Exception {
        User existing = existingUser("{bcrypt}existing");
        when(userRepository.findByEmailIgnoreCase("nanagyachie@gmail.com"))
                .thenReturn(Optional.of(existing));
        when(roleRepository.existsByUserIdAndRole(
                existing.getId(),
                SystemRole.SYSTEM_ADMIN
        )).thenReturn(true);

        seeder(true, "nanagyachie@gmail.com", "new-password")
                .run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
        verify(roleRepository, never()).save(any(SystemUserRole.class));
        assertThat(existing.getPasswordHash()).isEqualTo("{bcrypt}existing");
    }

    @Test
    void existingUserReceivesSystemAdminRoleAndActiveStatus() throws Exception {
        User existing = existingUser("{bcrypt}existing");
        existing.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmailIgnoreCase("nanagyachie@gmail.com"))
                .thenReturn(Optional.of(existing));
        when(roleRepository.existsByUserIdAndRole(
                existing.getId(),
                SystemRole.SYSTEM_ADMIN
        )).thenReturn(false);

        seeder(true, "nanagyachie@gmail.com", "")
                .run(new DefaultApplicationArguments());

        assertThat(existing.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(roleRepository).save(org.mockito.ArgumentMatchers.argThat(role ->
                role.getUser() == existing
                        && role.getRole() == SystemRole.SYSTEM_ADMIN
        ));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void missingPasswordPreventsNewSeededAdmin() {
        when(userRepository.findByEmailIgnoreCase("nanagyachie@gmail.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> seeder(true, "nanagyachie@gmail.com", "")
                .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUPERADMIN_PASSWORD is required");

        verify(userRepository, never()).save(any(User.class));
        verify(roleRepository, never()).save(any(SystemUserRole.class));
    }

    @Test
    void auditMetadataDoesNotContainSecretValues() throws Exception {
        when(userRepository.findByEmailIgnoreCase("nanagyachie@gmail.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("seed-password"))
                .thenReturn("{bcrypt}encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        seeder(true, "nanagyachie@gmail.com", "seed-password")
                .run(new DefaultApplicationArguments());

        verify(auditEventService).record(
                eq(null),
                eq("SYSTEM"),
                eq(null),
                eq(null),
                eq(com.researchassistant.audit.AuditEventType.SYSTEM_ADMIN_BOOTSTRAPPED),
                eq("User"),
                any(UUID.class),
                eq("{\"created\":true}")
        );
    }

    private SuperAdminSeeder seeder(
            boolean enabled,
            String email,
            String password
    ) {
        return new SuperAdminSeeder(
                new SuperAdminSeedProperties(
                        enabled,
                        email,
                        "0542011738",
                        password
                ),
                userRepository,
                roleRepository,
                passwordEncoder,
                auditEventService
        );
    }

    private User existingUser(String passwordHash) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("nanagyachie@gmail.com");
        user.setPasswordHash(passwordHash);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthenticationMethod(com.researchassistant.identity.entity.AuthenticationMethod.PASSWORD_OR_TOTP);
        user.setPhoneNumber("+233542011738");
        return user;
    }
}
