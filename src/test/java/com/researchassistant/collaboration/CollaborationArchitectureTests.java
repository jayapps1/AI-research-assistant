package com.researchassistant.collaboration;

import com.researchassistant.project.entity.ProjectRole;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CollaborationArchitectureTests {

    @Test
    void projectRolesIncludeReviewAndSupervisorWithoutRemovingCoreRoles() {
        assertThat(ProjectRole.values())
                .contains(ProjectRole.LEAD, ProjectRole.EDITOR, ProjectRole.REVIEWER, ProjectRole.SUPERVISOR, ProjectRole.VIEWER);
    }

    @Test
    void fakeAiProvidersRemainOutsideMainRuntimeSourceSet() throws Exception {
        Path main = Path.of("src", "main");
        try (var files = Files.walk(main)) {
            assertThat(files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("FakeAiGenerationProvider")
                                    || Files.readString(path).contains("FakeAiEmbeddingProvider");
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    })
                    .toList())
                    .isEmpty();
        }
    }

    @Test
    void collaborationMigrationStoresInvitationTokenHashesOnly() throws Exception {
        String migration = Files.readString(Path.of("src", "main", "resources", "db", "migration", "V19__project_collaboration_workflows.sql"));
        assertThat(migration).contains("token_hash VARCHAR(64) NOT NULL");
        assertThat(migration).doesNotContain("raw_token");
        assertThat(migration).doesNotContain("invitation_token VARCHAR");
    }
}
