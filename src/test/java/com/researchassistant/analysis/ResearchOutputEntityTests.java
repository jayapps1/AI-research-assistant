package com.researchassistant.analysis;

import com.researchassistant.analysis.entity.*;
import com.researchassistant.common.enums.ContentOrigin;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchOutputEntityTests {

    @Test
    void findingDefaultsPreserveDraftStatusAndUserOrigin() throws Exception {
        ResearchFinding finding = new ResearchFinding();
        finding.setTitle("Finding 1");
        finding.setFindingText("The deterministic result supports this finding.");

        invokeLifecycle(finding, "onCreate");

        assertThat(finding.getStatus()).isEqualTo(ResearchFindingStatus.DRAFT);
        assertThat(finding.getType()).isEqualTo(ResearchFindingType.OTHER);
        assertThat(finding.getOrigin()).isEqualTo(ContentOrigin.USER);
        assertThat(finding.getRevisionNumber()).isEqualTo(1);
        assertThat(finding.getStatement()).isEqualTo(finding.getFindingText());
    }

    @Test
    void recommendationDefaultsDoNotInflatePriority() throws Exception {
        ResearchRecommendation recommendation = new ResearchRecommendation();
        recommendation.setTitle("Recommendation");
        recommendation.setRecommendationText("Use the finding as the implementation basis.");

        invokeLifecycle(recommendation, "onCreate");

        assertThat(recommendation.getPriority()).isEqualTo(RecommendationPriority.MEDIUM);
        assertThat(recommendation.getStatus()).isEqualTo(ResearchRecommendationStatus.DRAFT);
        assertThat(recommendation.getType()).isEqualTo(ResearchRecommendationType.OTHER);
        assertThat(recommendation.getRecommendation()).isEqualTo(recommendation.getRecommendationText());
    }

    @Test
    void reportSectionTracksManualEditAndSourceStalenessFlags() {
        ResearchReportSection section = new ResearchReportSection();
        section.setSourceArtifactType("ResearchFinding");
        section.setSourceOutOfDate(true);
        section.setManuallyEdited(true);

        assertThat(section.isSourceOutOfDate()).isTrue();
        assertThat(section.isManuallyEdited()).isTrue();
        assertThat(section.getSourceArtifactType()).isEqualTo("ResearchFinding");
    }

    private void invokeLifecycle(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}
