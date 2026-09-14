package com.researchassistant.fieldwork;

import com.researchassistant.dataset.model.*;
import com.researchassistant.dataset.repository.*;
import com.researchassistant.dataset.service.DatasetValidationService;
import com.researchassistant.ethics.dto.EthicsDtos.EthicsReadinessResponse;
import com.researchassistant.ethics.model.ConsentForm;
import com.researchassistant.ethics.model.EthicsApproval;
import com.researchassistant.ethics.model.EthicsProtocol;
import com.researchassistant.ethics.model.ParticipantConsent;
import com.researchassistant.ethics.repository.*;
import com.researchassistant.ethics.service.ConsentPolicyService;
import com.researchassistant.ethics.service.EthicsReadinessService;
import com.researchassistant.instruments.entity.ResearchInstrument;
import com.researchassistant.instruments.entity.ResearchInstrumentStatus;
import com.researchassistant.instruments.repository.ResearchInstrumentRepository;
import com.researchassistant.methodology.entity.Methodology;
import com.researchassistant.methodology.entity.MethodologyStatus;
import com.researchassistant.methodology.entity.SamplingPlan;
import com.researchassistant.methodology.repository.MethodologyRepository;
import com.researchassistant.methodology.repository.SamplingPlanRepository;
import com.researchassistant.participant.model.Participant;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.workspace.entity.Workspace;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FieldworkCoreServiceTests {

    @Test
    void readinessRejectsDraftProtocolAsNotReady() {
        UUID projectId = UUID.randomUUID();
        EthicsProtocol protocol = protocol(projectId, EthicsProtocol.Status.DRAFT);
        EthicsReadinessService service = readinessService(protocol, null, true, true, true, true);

        EthicsReadinessResponse response = service.evaluate(projectId);

        assertThat(response.status()).isEqualTo("NOT_READY");
        assertThat(response.blockingIssues()).contains("Latest ethics protocol is not approved or conditionally approved.");
    }

    @Test
    void readinessDetectsExpiredApproval() {
        UUID projectId = UUID.randomUUID();
        EthicsApproval approval = new EthicsApproval();
        approval.setExpiryDate(LocalDate.now().minusDays(1));
        EthicsReadinessService service = readinessService(protocol(projectId, EthicsProtocol.Status.APPROVED), approval, true, true, true, true);

        EthicsReadinessResponse response = service.evaluate(projectId);

        assertThat(response.status()).isEqualTo("NOT_READY");
        assertThat(response.blockingIssues()).contains("Ethics approval has expired.");
    }

    @Test
    void activeConsentAllowsCollection() {
        Participant participant = participant(Participant.Status.ENROLLED);
        ParticipantConsentRepository repository = mock(ParticipantConsentRepository.class);
        ParticipantConsent consent = new ParticipantConsent();
        consent.setDecision(ParticipantConsent.Decision.CONSENTED);
        when(repository.findFirstByParticipantIdOrderByConsentedAtDesc(participant.getId())).thenReturn(Optional.of(consent));

        new ConsentPolicyService(repository).requireValidConsent(participant);

        verify(repository).findFirstByParticipantIdOrderByConsentedAtDesc(participant.getId());
    }

    @Test
    void declinedConsentBlocksCollection() {
        Participant participant = participant(Participant.Status.ENROLLED);
        ParticipantConsentRepository repository = mock(ParticipantConsentRepository.class);
        ParticipantConsent consent = new ParticipantConsent();
        consent.setDecision(ParticipantConsent.Decision.DECLINED);
        when(repository.findFirstByParticipantIdOrderByConsentedAtDesc(participant.getId())).thenReturn(Optional.of(consent));

        assertThatThrownBy(() -> new ConsentPolicyService(repository).requireValidConsent(participant))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active consent");
    }

    @Test
    void withdrawnParticipantBlocksCollectionDespitePriorConsent() {
        Participant participant = participant(Participant.Status.WITHDRAWN);
        ParticipantConsentRepository repository = mock(ParticipantConsentRepository.class);
        ParticipantConsent consent = new ParticipantConsent();
        consent.setDecision(ParticipantConsent.Decision.CONSENTED);
        when(repository.findFirstByParticipantIdOrderByConsentedAtDesc(participant.getId())).thenReturn(Optional.of(consent));

        assertThatThrownBy(() -> new ConsentPolicyService(repository).requireValidConsent(participant))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void datasetValidationReportsMultipleLogicalValues() {
        ResearchDataset dataset = dataset();
        DatasetVariable variable = variable(dataset, DatasetVariable.VariableType.INTEGER, true);
        DatasetRecord record = record(dataset);
        DatasetValue value = new DatasetValue();
        value.setRecord(record);
        value.setVariable(variable);
        value.setIntegerValue(1L);
        value.setStringValue("one");
        DatasetValidationIssueRepository issueRepository = mock(DatasetValidationIssueRepository.class);
        DatasetValidationService service = validationService(List.of(variable), List.of(value), issueRepository);

        service.validate(dataset);

        ArgumentCaptor<DatasetValidationIssue> captor = ArgumentCaptor.forClass(DatasetValidationIssue.class);
        verify(issueRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("INVALID_VALUE");
        assertThat(dataset.getStatus()).isEqualTo(ResearchDataset.Status.INVALID);
    }

    @Test
    void missingRequiredDatasetValueIsInvalid() {
        ResearchDataset dataset = dataset();
        DatasetVariable variable = variable(dataset, DatasetVariable.VariableType.STRING, false);
        DatasetRecord record = record(dataset);
        DatasetValue value = new DatasetValue();
        value.setRecord(record);
        value.setVariable(variable);
        value.setMissing(true);
        DatasetValidationIssueRepository issueRepository = mock(DatasetValidationIssueRepository.class);
        DatasetValidationService service = validationService(List.of(variable), List.of(value), issueRepository);

        service.validate(dataset);

        verify(issueRepository).save(any(DatasetValidationIssue.class));
        assertThat(dataset.getStatus()).isEqualTo(ResearchDataset.Status.INVALID);
    }

    @Test
    void validDatasetValuesMarkDatasetReady() {
        ResearchDataset dataset = dataset();
        DatasetVariable variable = variable(dataset, DatasetVariable.VariableType.INTEGER, false);
        DatasetRecord record = record(dataset);
        DatasetValue value = new DatasetValue();
        value.setRecord(record);
        value.setVariable(variable);
        value.setIntegerValue(42L);
        DatasetValidationIssueRepository issueRepository = mock(DatasetValidationIssueRepository.class);
        DatasetValidationService service = validationService(List.of(variable), List.of(value), issueRepository);

        service.validate(dataset);

        verify(issueRepository, never()).save(any());
        assertThat(dataset.getStatus()).isEqualTo(ResearchDataset.Status.READY);
    }

    private EthicsReadinessService readinessService(EthicsProtocol protocol, EthicsApproval approval, boolean consent, boolean methodology, boolean sampling, boolean instrument) {
        EthicsProtocolRepository protocols = mock(EthicsProtocolRepository.class);
        EthicsApprovalRepository approvals = mock(EthicsApprovalRepository.class);
        ConsentFormRepository consentForms = mock(ConsentFormRepository.class);
        ResearchInstrumentRepository instruments = mock(ResearchInstrumentRepository.class);
        MethodologyRepository methodologies = mock(MethodologyRepository.class);
        SamplingPlanRepository samplingPlans = mock(SamplingPlanRepository.class);
        when(protocols.findFirstByProjectIdOrderByCreatedAtDesc(any())).thenReturn(Optional.ofNullable(protocol));
        when(approvals.findFirstByProtocolIdOrderByCreatedAtDesc(any())).thenReturn(Optional.ofNullable(approval));
        when(consentForms.existsByProjectIdAndStatus(any(), eq(ConsentForm.Status.ACTIVE))).thenReturn(consent);
        when(methodologies.findByProjectIdAndStatus(any(), eq(MethodologyStatus.ACTIVE))).thenReturn(methodology ? Optional.of(new Methodology()) : Optional.empty());
        SamplingPlan plan = new SamplingPlan();
        Methodology m = new Methodology();
        m.setProject(project(protocol == null ? UUID.randomUUID() : protocol.getProject().getId()));
        plan.setMethodology(m);
        when(samplingPlans.findAll()).thenReturn(sampling ? List.of(plan) : List.of());
        ResearchInstrument ri = new ResearchInstrument();
        ri.setStatus(ResearchInstrumentStatus.ACTIVE);
        when(instruments.findAllByProjectIdOrderByCreatedAtDesc(any())).thenReturn(instrument ? List.of(ri) : List.of());
        return new EthicsReadinessService(protocols, approvals, consentForms, instruments, methodologies, samplingPlans);
    }

    private DatasetValidationService validationService(List<DatasetVariable> variables, List<DatasetValue> values, DatasetValidationIssueRepository issueRepository) {
        DatasetVariableRepository variableRepository = mock(DatasetVariableRepository.class);
        DatasetRecordRepository recordRepository = mock(DatasetRecordRepository.class);
        DatasetValueRepository valueRepository = mock(DatasetValueRepository.class);
        DatasetVariableCategoryRepository categoryRepository = mock(DatasetVariableCategoryRepository.class);
        when(variableRepository.findAllByDatasetIdOrderByDisplayOrderAsc(any())).thenReturn(variables);
        when(valueRepository.findAllByRecordDatasetId(any())).thenReturn(values);
        when(issueRepository.findAllByDatasetId(any(), any(Pageable.class))).thenReturn(Page.empty());
        return new DatasetValidationService(variableRepository, recordRepository, valueRepository, categoryRepository, issueRepository);
    }

    private EthicsProtocol protocol(UUID projectId, EthicsProtocol.Status status) {
        EthicsProtocol protocol = new EthicsProtocol();
        protocol.setId(UUID.randomUUID());
        protocol.setProject(project(projectId));
        protocol.setStatus(status);
        protocol.setConfidentialityPlan("confidentiality");
        protocol.setDataProtectionPlan("data protection");
        protocol.setWithdrawalProcedure("withdrawal");
        return protocol;
    }

    private Participant participant(Participant.Status status) {
        Participant participant = new Participant();
        participant.setId(UUID.randomUUID());
        participant.setStatus(status);
        return participant;
    }

    private ResearchDataset dataset() {
        ResearchDataset dataset = new ResearchDataset();
        dataset.setId(UUID.randomUUID());
        return dataset;
    }

    private DatasetVariable variable(ResearchDataset dataset, DatasetVariable.VariableType type, boolean nullable) {
        DatasetVariable variable = new DatasetVariable();
        variable.setId(UUID.randomUUID());
        variable.setDataset(dataset);
        variable.setVariableName("v1");
        variable.setType(type);
        variable.setNullable(nullable);
        return variable;
    }

    private DatasetRecord record(ResearchDataset dataset) {
        DatasetRecord record = new DatasetRecord();
        record.setDataset(dataset);
        record.setRowNumber(1);
        return record;
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
