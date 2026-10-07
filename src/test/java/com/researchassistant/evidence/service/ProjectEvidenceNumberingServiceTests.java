package com.researchassistant.evidence.service;

import com.researchassistant.analysis.entity.ResearchReportChapter;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.evidence.entity.EvidenceType;
import com.researchassistant.evidence.entity.ProjectEvidence;
import com.researchassistant.evidence.repository.ProjectEvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectEvidenceNumberingServiceTests {

    private ProjectEvidenceRepository evidenceRepository;
    private ProjectEvidenceNumberingService numberingService;

    @BeforeEach
    void setUp() {
        evidenceRepository = Mockito.mock(ProjectEvidenceRepository.class);
        numberingService = new ProjectEvidenceNumberingService(evidenceRepository);
    }

    @Test
    @DisplayName("Computes dynamic semantic figure and table labels such as Figure 4.1, Figure 4.2, and Table 4.1")
    void computesDynamicLabelsForChapter() {
        UUID chapterId = UUID.randomUUID();
        ResearchReportChapter chapter = new ResearchReportChapter();
        chapter.setId(chapterId);
        chapter.setChapterNumber(4);
        chapter.setTitle("Chapter Four: Results and Discussion");

        ResearchReportSection section1 = new ResearchReportSection();
        section1.setId(UUID.randomUUID());
        section1.setSectionNumber("4.1");
        section1.setHeading("System Implementation & Screenshots");
        section1.setChapter(chapter);

        ResearchReportSection section2 = new ResearchReportSection();
        section2.setId(UUID.randomUUID());
        section2.setSectionNumber("4.2");
        section2.setHeading("Performance & Benchmark Results");
        section2.setChapter(chapter);

        // Figure 1: Screenshot in 4.1
        ProjectEvidence ev1 = new ProjectEvidence();
        ev1.setId(UUID.randomUUID());
        ev1.setEvidenceType(EvidenceType.SCREENSHOT);
        ev1.setSection(section1);

        // Figure 2: Architecture diagram in 4.1
        ProjectEvidence ev2 = new ProjectEvidence();
        ev2.setId(UUID.randomUUID());
        ev2.setEvidenceType(EvidenceType.ARCHITECTURE_DIAGRAM);
        ev2.setSection(section1);

        // Table 1: Structured Table in 4.2
        ProjectEvidence ev3 = new ProjectEvidence();
        ev3.setId(UUID.randomUUID());
        ev3.setEvidenceType(EvidenceType.TABLE);
        ev3.setSection(section2);

        // Figure 3: Chart in 4.2
        ProjectEvidence ev4 = new ProjectEvidence();
        ev4.setId(UUID.randomUUID());
        ev4.setEvidenceType(EvidenceType.CHART);
        ev4.setSection(section2);

        Map<UUID, String> labels = numberingService.computeLabels(List.of(ev1, ev2, ev3, ev4));

        assertThat(labels.get(ev1.getId())).isEqualTo("Figure 4.1");
        assertThat(labels.get(ev2.getId())).isEqualTo("Figure 4.2");
        assertThat(labels.get(ev3.getId())).isEqualTo("Table 4.1");
        assertThat(labels.get(ev4.getId())).isEqualTo("Figure 4.3");
    }

    @Test
    @DisplayName("Resolves fallback label safely when evidence is not yet in computed map")
    void resolvesFallbackLabelWhenNotInMap() {
        ResearchReportChapter chapter = new ResearchReportChapter();
        chapter.setId(UUID.randomUUID());
        chapter.setChapterNumber(3);
        chapter.setTitle("Chapter Three: Methodology");

        ResearchReportSection section = new ResearchReportSection();
        section.setId(UUID.randomUUID());
        section.setChapter(chapter);

        ProjectEvidence figure = new ProjectEvidence();
        figure.setId(UUID.randomUUID());
        figure.setEvidenceType(EvidenceType.DIAGRAM);
        figure.setSection(section);

        ProjectEvidence table = new ProjectEvidence();
        table.setId(UUID.randomUUID());
        table.setEvidenceType(EvidenceType.TABLE);
        table.setSection(section);

        assertThat(numberingService.resolveLabel(figure, Map.of())).isEqualTo("Figure 3.1");
        assertThat(numberingService.resolveLabel(table, Map.of())).isEqualTo("Table 3.1");
    }

    @Test
    @DisplayName("Identifies table types correctly")
    void identifiesTableTypes() {
        assertThat(numberingService.isTableType(EvidenceType.TABLE)).isTrue();
        assertThat(numberingService.isTableType(EvidenceType.SCREENSHOT)).isFalse();
        assertThat(numberingService.isTableType(EvidenceType.DIAGRAM)).isFalse();
        assertThat(numberingService.isTableType(EvidenceType.CHART)).isFalse();
    }
}
