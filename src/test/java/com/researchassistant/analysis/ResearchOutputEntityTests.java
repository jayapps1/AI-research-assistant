package com.researchassistant.analysis;

import com.researchassistant.analysis.entity.*;
import com.researchassistant.literature.entity.LiteratureMatrix;
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

    @Test
    void reportSectionSupportsHierarchicalNestingAndSystemFlags() {
        ResearchReportSection parent = new ResearchReportSection();
        parent.setHeading("Literature Review");
        parent.setSectionNumber("2.1");
        parent.setRequired(true);
        parent.setSystemDefined(true);

        ResearchReportSection child = new ResearchReportSection();
        child.setParentSection(parent);
        child.setHeading("Thematic Analysis");
        child.setSectionNumber("2.1.1");
        child.setRequired(false);
        child.setSystemDefined(false);
        child.setAiEnabled(true);

        assertThat(child.getParentSection()).isEqualTo(parent);
        assertThat(child.getSectionNumber()).isEqualTo("2.1.1");
        assertThat(child.isRequired()).isFalse();
        assertThat(child.isSystemDefined()).isFalse();
        assertThat(child.isAiEnabled()).isTrue();
        assertThat(parent.isRequired()).isTrue();
    }

    @Test
    void researchReportHoldsLiteratureMatrixInclusionAndUncitedReferencesFlags() {
        ResearchReport report = new ResearchReport();
        report.setIncludeUncitedReferences(true);
        report.setLiteratureMatrixInclusion("CHAPTER_TWO");

        assertThat(report.isIncludeUncitedReferences()).isTrue();
        assertThat(report.getLiteratureMatrixInclusion()).isEqualTo("CHAPTER_TWO");
    }

    @Test
    void literatureMatrixEntityHoldsExtractedTable() {
        LiteratureMatrix matrix = new LiteratureMatrix();
        matrix.setTitle("Evidence Matrix");
        matrix.setMarkdownTable("| Source | Objective | Findings |\n|---|---|---|");

        assertThat(matrix.getTitle()).isEqualTo("Evidence Matrix");
        assertThat(matrix.getMarkdownTable()).contains("| Source | Objective | Findings |");
    }

    private void invokeLifecycle(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}
