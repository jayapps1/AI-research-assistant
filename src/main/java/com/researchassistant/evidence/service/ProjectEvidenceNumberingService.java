package com.researchassistant.evidence.service;

import com.researchassistant.analysis.entity.ResearchReportChapter;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.evidence.entity.EvidenceType;
import com.researchassistant.evidence.entity.ProjectEvidence;
import com.researchassistant.evidence.repository.ProjectEvidenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProjectEvidenceNumberingService {

    private final ProjectEvidenceRepository evidenceRepository;

    public ProjectEvidenceNumberingService(ProjectEvidenceRepository evidenceRepository) {
        this.evidenceRepository = evidenceRepository;
    }

    /**
     * Determines whether an evidence type represents a Table or a Figure.
     */
    public boolean isTableType(EvidenceType type) {
        return type == EvidenceType.TABLE;
    }

    /**
     * Computes the map of Evidence ID -> dynamic label (e.g. "Figure 4.1", "Table 4.1")
     * based on chapter hierarchy, section ordering, and display order.
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> computeDynamicLabelsForProject(UUID projectId) {
        List<ProjectEvidence> allEvidence = evidenceRepository.findAllAssignedToProjectHierarchy(projectId);
        return computeLabels(allEvidence);
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> computeDynamicLabelsForReport(UUID reportId) {
        List<ProjectEvidence> allEvidence = evidenceRepository.findAllAssignedToReportHierarchy(reportId);
        return computeLabels(allEvidence);
    }

    public Map<UUID, String> computeLabels(List<ProjectEvidence> assignedEvidence) {
        Map<UUID, String> labels = new HashMap<>();

        // Group by Chapter ID
        Map<UUID, List<ProjectEvidence>> byChapter = new LinkedHashMap<>();
        for (ProjectEvidence e : assignedEvidence) {
            ResearchReportSection sec = e.getSection();
            if (sec != null && sec.getChapter() != null) {
                byChapter.computeIfAbsent(sec.getChapter().getId(), k -> new ArrayList<>()).add(e);
            }
        }

        int unnumberedChapterSeq = 1;
        for (Map.Entry<UUID, List<ProjectEvidence>> entry : byChapter.entrySet()) {
            List<ProjectEvidence> chapterEvidence = entry.getValue();
            if (chapterEvidence.isEmpty()) continue;

            ResearchReportChapter chapter = chapterEvidence.get(0).getSection().getChapter();
            int chapterPrefix = (chapter.getChapterNumber() != null && chapter.getChapterNumber() > 0)
                    ? chapter.getChapterNumber()
                    : (chapter.getDisplayOrder() > 0 ? chapter.getDisplayOrder() : unnumberedChapterSeq++);

            int figureCount = 1;
            int tableCount = 1;

            for (ProjectEvidence e : chapterEvidence) {
                if (isTableType(e.getEvidenceType())) {
                    labels.put(e.getId(), "Table " + chapterPrefix + "." + tableCount);
                    tableCount++;
                } else {
                    labels.put(e.getId(), "Figure " + chapterPrefix + "." + figureCount);
                    figureCount++;
                }
            }
        }

        return labels;
    }

    /**
     * Computes single label for an individual evidence item if possible.
     */
    public String resolveLabel(ProjectEvidence evidence, Map<UUID, String> computedLabels) {
        if (computedLabels != null && computedLabels.containsKey(evidence.getId())) {
            return computedLabels.get(evidence.getId());
        }
        if (evidence.getSection() != null && evidence.getSection().getChapter() != null) {
            ResearchReportChapter ch = evidence.getSection().getChapter();
            int num = (ch.getChapterNumber() != null && ch.getChapterNumber() > 0)
                    ? ch.getChapterNumber()
                    : (ch.getDisplayOrder() > 0 ? ch.getDisplayOrder() : 1);
            return isTableType(evidence.getEvidenceType()) ? "Table " + num + ".1" : "Figure " + num + ".1";
        }
        return isTableType(evidence.getEvidenceType()) ? "Table" : "Figure";
    }
}
