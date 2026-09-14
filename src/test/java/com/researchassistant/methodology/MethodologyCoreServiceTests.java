package com.researchassistant.methodology;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.methodology.dto.MethodologyRequests.CalculateSampleSizeRequest;
import com.researchassistant.methodology.entity.*;
import com.researchassistant.methodology.exception.MethodologyValidationException;
import com.researchassistant.methodology.service.MethodologyConsistencyService;
import com.researchassistant.methodology.service.SampleSizeCalculator;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.researchdesign.entity.ResearchObjective;
import com.researchassistant.workspace.entity.Workspace;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodologyCoreServiceTests {

    @Test
    void cochranProportionUsesCeilingAndRecordsDefaultP() {
        var result = new SampleSizeCalculator().calculate(
                UUID.randomUUID(),
                new CalculateSampleSizeRequest(
                        SampleSizeMethod.COCHRAN_PROPORTION,
                        null,
                        0.95,
                        0.05,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        assertThat(result.initialSampleSize()).isEqualTo(385);
        assertThat(result.adjustedSampleSize()).isEqualTo(385);
        assertThat(result.assumptions()).contains("p=0.5 defaulted");
    }

    @Test
    void cochranFinitePopulationCorrectionIsApplied() {
        var result = new SampleSizeCalculator().calculate(
                UUID.randomUUID(),
                new CalculateSampleSizeRequest(
                        SampleSizeMethod.COCHRAN_FINITE_POPULATION,
                        1000L,
                        0.95,
                        0.05,
                        0.5,
                        null,
                        null,
                        null,
                        null
                )
        );

        assertThat(result.initialSampleSize()).isEqualTo(278);
    }

    @Test
    void yamaneAndNonResponseAdjustmentUseCeiling() {
        var result = new SampleSizeCalculator().calculate(
                UUID.randomUUID(),
                new CalculateSampleSizeRequest(
                        SampleSizeMethod.YAMANE,
                        1000L,
                        null,
                        0.05,
                        null,
                        null,
                        0.8,
                        null,
                        null
                )
        );

        assertThat(result.initialSampleSize()).isEqualTo(286);
        assertThat(result.adjustedSampleSize()).isEqualTo(358);
    }

    @Test
    void invalidSamplingInputsAreRejected() {
        assertThatThrownBy(() -> new SampleSizeCalculator().calculate(
                UUID.randomUUID(),
                new CalculateSampleSizeRequest(
                        SampleSizeMethod.YAMANE,
                        1000L,
                        null,
                        0.0,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        )).isInstanceOf(MethodologyValidationException.class);
    }

    @Test
    void qualitativeJustificationDoesNotRequirePopulationSize() {
        var result = new SampleSizeCalculator().calculate(
                UUID.randomUUID(),
                new CalculateSampleSizeRequest(
                        SampleSizeMethod.QUALITATIVE_JUSTIFICATION,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        12,
                        "Recruit until thematic saturation is assessed during fieldwork."
                )
        );

        assertThat(result.adjustedSampleSize()).isEqualTo(12);
        assertThat(result.formulaDescription()).contains("no quantitative formula");
    }

    @Test
    void crossProjectObjectiveIsRejected() {
        UUID projectId = UUID.randomUUID();
        ResearchObjective objective = new ResearchObjective();
        objective.setProject(project(UUID.randomUUID()));

        assertThatThrownBy(() -> new MethodologyConsistencyService()
                .requireObjectiveInProject(objective, projectId))
                .isInstanceOf(MethodologyValidationException.class);
    }

    @Test
    void samplingPlanPopulationMustBelongToSameMethodology() {
        Methodology expected = methodology(UUID.randomUUID());
        StudyPopulation population = new StudyPopulation();
        population.setMethodology(methodology(UUID.randomUUID()));

        assertThatThrownBy(() -> new MethodologyConsistencyService()
                .requirePopulationInMethodology(population, expected))
                .isInstanceOf(MethodologyValidationException.class);
    }

    @Test
    void theoreticalEvidenceVersionMustBelongToDocument() {
        UUID projectId = UUID.randomUUID();
        Document document = new Document();
        document.setProject(project(projectId));
        document.setId(UUID.randomUUID());
        DocumentVersion version = new DocumentVersion();
        Document otherDocument = new Document();
        otherDocument.setId(UUID.randomUUID());
        version.setDocument(otherDocument);

        assertThatThrownBy(() -> new MethodologyConsistencyService()
                .requireDocumentEvidenceInProject(document, version, null, null, projectId))
                .isInstanceOf(MethodologyValidationException.class);
    }

    private Methodology methodology(UUID id) {
        Methodology methodology = new Methodology();
        methodology.setId(id);
        methodology.setProject(project(UUID.randomUUID()));
        return methodology;
    }

    private ResearchProject project(UUID id) {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        ResearchProject project = new ResearchProject();
        project.setId(id);
        project.setWorkspace(workspace);
        return project;
    }
}
