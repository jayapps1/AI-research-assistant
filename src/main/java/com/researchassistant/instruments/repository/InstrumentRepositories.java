package com.researchassistant.instruments.repository;

import com.researchassistant.instruments.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InstrumentRepositories {
    private InstrumentRepositories() {}

    public interface ResearchInstrumentRepository extends JpaRepository<ResearchInstrument, UUID> {
        List<ResearchInstrument> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);
    }

    public interface QuestionnaireRepository extends JpaRepository<Questionnaire, UUID> {
        List<Questionnaire> findAllByInstrumentProjectId(UUID projectId);
        Optional<Questionnaire> findByInstrumentId(UUID instrumentId);
    }

    public interface QuestionnaireSectionRepository extends JpaRepository<QuestionnaireSection, UUID> {
        List<QuestionnaireSection> findAllByQuestionnaireIdOrderByDisplayOrderAsc(UUID questionnaireId);
        long countByQuestionnaireId(UUID questionnaireId);
    }

    public interface QuestionnaireItemRepository extends JpaRepository<QuestionnaireItem, UUID> {
        List<QuestionnaireItem> findAllBySectionQuestionnaireIdOrderByDisplayOrderAsc(UUID questionnaireId);
        List<QuestionnaireItem> findAllBySectionIdOrderByDisplayOrderAsc(UUID sectionId);
        long countBySectionQuestionnaireId(UUID questionnaireId);
    }

    public interface QuestionnaireOptionRepository extends JpaRepository<QuestionnaireOption, UUID> {
        List<QuestionnaireOption> findAllByItemIdOrderByDisplayOrderAsc(UUID itemId);
        long countByItemId(UUID itemId);
    }

    public interface ResponseScaleRepository extends JpaRepository<ResponseScale, UUID> {
        List<ResponseScale> findAllByProjectId(UUID projectId);
    }

    public interface ResponseScaleOptionRepository extends JpaRepository<ResponseScaleOption, UUID> {
        List<ResponseScaleOption> findAllByScaleIdOrderByDisplayOrderAsc(UUID scaleId);
        long countByScaleId(UUID scaleId);
    }

    public interface InterviewGuideRepository extends JpaRepository<InterviewGuide, UUID> {
        List<InterviewGuide> findAllByInstrumentProjectId(UUID projectId);
    }

    public interface InterviewGuideSectionRepository extends JpaRepository<InterviewGuideSection, UUID> {
        List<InterviewGuideSection> findAllByGuideIdOrderByDisplayOrderAsc(UUID guideId);
        long countByGuideId(UUID guideId);
    }

    public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {
        long countBySectionGuideId(UUID guideId);
    }

    public interface InterviewProbeRepository extends JpaRepository<InterviewProbe, UUID> {}

    public interface FocusGroupGuideRepository extends JpaRepository<FocusGroupGuide, UUID> {
        List<FocusGroupGuide> findAllByInstrumentProjectId(UUID projectId);
    }

    public interface FocusGroupSectionRepository extends JpaRepository<FocusGroupSection, UUID> {
        long countByGuideId(UUID guideId);
    }

    public interface FocusGroupQuestionRepository extends JpaRepository<FocusGroupQuestion, UUID> {
        long countBySectionGuideId(UUID guideId);
    }

    public interface FocusGroupProbeRepository extends JpaRepository<FocusGroupProbe, UUID> {}

    public interface ObservationChecklistRepository extends JpaRepository<ObservationChecklist, UUID> {
        List<ObservationChecklist> findAllByInstrumentProjectId(UUID projectId);
    }

    public interface ObservationChecklistSectionRepository extends JpaRepository<ObservationChecklistSection, UUID> {
        long countByChecklistId(UUID checklistId);
    }

    public interface ObservationItemRepository extends JpaRepository<ObservationItem, UUID> {
        long countBySectionChecklistId(UUID checklistId);
    }

    public interface ObservationOptionRepository extends JpaRepository<ObservationOption, UUID> {
        long countByItemId(UUID itemId);
    }

    public interface InstrumentPilotStudyRepository extends JpaRepository<InstrumentPilotStudy, UUID> {}
    public interface InstrumentValidityAssessmentRepository extends JpaRepository<InstrumentValidityAssessment, UUID> {}
    public interface InstrumentReliabilityAssessmentRepository extends JpaRepository<InstrumentReliabilityAssessment, UUID> {}
    public interface ExpertReviewRepository extends JpaRepository<ExpertReview, UUID> {}
    public interface ExpertReviewItemRatingRepository extends JpaRepository<ExpertReviewItemRating, UUID> {
        List<ExpertReviewItemRating> findAllByExpertReviewInstrumentIdAndCriterion(UUID instrumentId, ExpertRatingCriterion criterion);
    }
}
