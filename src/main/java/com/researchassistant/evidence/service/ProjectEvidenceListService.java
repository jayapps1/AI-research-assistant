package com.researchassistant.evidence.service;

import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.analysis.entity.ResearchReportChapter;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.analysis.repository.ResearchReportChapterRepository;
import com.researchassistant.analysis.repository.ResearchReportRepository;
import com.researchassistant.analysis.repository.ResearchReportSectionRepository;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.evidence.dto.ProjectEvidenceDtos.ListOfFiguresItem;
import com.researchassistant.evidence.dto.ProjectEvidenceDtos.ListOfTablesItem;
import com.researchassistant.evidence.entity.EvidenceType;
import com.researchassistant.evidence.entity.ProjectEvidence;
import com.researchassistant.evidence.repository.ProjectEvidenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProjectEvidenceListService {

    private static final int WORDS_PER_PAGE = 300;

    private final ProjectEvidenceRepository evidenceRepository;
    private final ProjectEvidenceNumberingService numberingService;
    private final ResearchReportRepository reportRepository;
    private final ResearchReportChapterRepository chapterRepository;
    private final ResearchReportSectionRepository sectionRepository;

    public ProjectEvidenceListService(
            ProjectEvidenceRepository evidenceRepository,
            ProjectEvidenceNumberingService numberingService,
            ResearchReportRepository reportRepository,
            ResearchReportChapterRepository chapterRepository,
            ResearchReportSectionRepository sectionRepository
    ) {
        this.evidenceRepository = evidenceRepository;
        this.numberingService = numberingService;
        this.reportRepository = reportRepository;
        this.chapterRepository = chapterRepository;
        this.sectionRepository = sectionRepository;
    }

    /**
     * Deterministically generates List of Figures for a report.
     */
    @Transactional(readOnly = true)
    public List<ListOfFiguresItem> generateListOfFigures(UUID reportId) {
        ResearchReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        Map<UUID, Integer> sectionPageNumbers = computeSectionPageNumbers(report);
        Map<UUID, String> labels = numberingService.computeDynamicLabelsForReport(reportId);
        List<ProjectEvidence> assigned = evidenceRepository.findAllAssignedToReportHierarchy(reportId);

        List<ListOfFiguresItem> figures = new ArrayList<>();
        for (ProjectEvidence e : assigned) {
            if (numberingService.isTableType(e.getEvidenceType())) {
                continue;
            }
            ResearchReportSection sec = e.getSection();
            int page = sec != null ? sectionPageNumbers.getOrDefault(sec.getId(), 1) : 1;
            String label = labels.getOrDefault(e.getId(), "Figure");
            String chapTitle = (sec != null && sec.getChapter() != null) ? sec.getChapter().getTitle() : "";
            String secNum = sec != null ? sec.getSectionNumber() : "";
            String secHead = sec != null ? sec.getHeading() : "";

            figures.add(new ListOfFiguresItem(
                    e.getId(),
                    label,
                    e.getCaption() != null ? e.getCaption() : "Untitled Figure",
                    page,
                    sec != null ? sec.getId() : null,
                    secNum,
                    secHead,
                    chapTitle
            ));
        }
        return figures;
    }

    /**
     * Deterministically generates List of Tables for a report.
     */
    @Transactional(readOnly = true)
    public List<ListOfTablesItem> generateListOfTables(UUID reportId) {
        ResearchReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        Map<UUID, Integer> sectionPageNumbers = computeSectionPageNumbers(report);
        Map<UUID, String> labels = numberingService.computeDynamicLabelsForReport(reportId);
        List<ProjectEvidence> assigned = evidenceRepository.findAllAssignedToReportHierarchy(reportId);

        List<ListOfTablesItem> tables = new ArrayList<>();
        for (ProjectEvidence e : assigned) {
            if (!numberingService.isTableType(e.getEvidenceType())) {
                continue;
            }
            ResearchReportSection sec = e.getSection();
            int page = sec != null ? sectionPageNumbers.getOrDefault(sec.getId(), 1) : 1;
            String label = labels.getOrDefault(e.getId(), "Table");
            String chapTitle = (sec != null && sec.getChapter() != null) ? sec.getChapter().getTitle() : "";
            String secNum = sec != null ? sec.getSectionNumber() : "";
            String secHead = sec != null ? sec.getHeading() : "";

            tables.add(new ListOfTablesItem(
                    e.getId(),
                    label,
                    e.getCaption() != null ? e.getCaption() : "Untitled Table",
                    page,
                    sec != null ? sec.getId() : null,
                    secNum,
                    secHead,
                    chapTitle
            ));
        }
        return tables;
    }

    /**
     * Reuses deterministic word-count pagination logic from ReportDynamicTocService.
     */
    private Map<UUID, Integer> computeSectionPageNumbers(ResearchReport report) {
        Map<UUID, Integer> pages = new HashMap<>();
        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId());
        List<ResearchReportSection> allSections = sectionRepository.findAllByReportIdOrderByChapterDisplayOrderAscDisplayOrderAsc(report.getId());

        Map<UUID, List<ResearchReportSection>> sectionsByChapterId = new HashMap<>();
        for (ResearchReportSection sec : allSections) {
            sectionsByChapterId.computeIfAbsent(sec.getChapter().getId(), k -> new ArrayList<>()).add(sec);
        }

        int currentPage = 1;
        for (ResearchReportChapter chapter : chapters) {
            List<ResearchReportSection> topSections = sectionsByChapterId.getOrDefault(chapter.getId(), List.of());
            topSections.sort(Comparator.comparingInt(ResearchReportSection::getDisplayOrder));

            for (ResearchReportSection sec : topSections) {
                pages.put(sec.getId(), currentPage);
                int words = countWords(sec.getContent());
                currentPage += Math.max(1, words / WORDS_PER_PAGE);
            }
        }
        return pages;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        String[] words = text.trim().split("\\s+");
        return words.length;
    }
}
