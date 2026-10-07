package com.researchassistant.template.service;

import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.analysis.entity.ResearchReportChapter;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.analysis.repository.ResearchReportChapterRepository;
import com.researchassistant.analysis.repository.ResearchReportRepository;
import com.researchassistant.analysis.repository.ResearchReportSectionRepository;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.template.dto.AcademicTemplateDtos.DynamicTableOfContentsResponse;
import com.researchassistant.template.dto.AcademicTemplateDtos.TocChapterItem;
import com.researchassistant.template.dto.AcademicTemplateDtos.TocSectionItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReportDynamicTocService {

    private static final int WORDS_PER_PAGE = 300;

    private final ResearchReportRepository reportRepository;
    private final ResearchReportChapterRepository chapterRepository;
    private final ResearchReportSectionRepository sectionRepository;

    public ReportDynamicTocService(
            ResearchReportRepository reportRepository,
            ResearchReportChapterRepository chapterRepository,
            ResearchReportSectionRepository sectionRepository
    ) {
        this.reportRepository = reportRepository;
        this.chapterRepository = chapterRepository;
        this.sectionRepository = sectionRepository;
    }

    /**
     * Builds dynamic table of contents reflecting the current database hierarchy of chapters and sections.
     * Computes dynamic page numbering based on accumulated content lengths and chapter page breaks.
     */
    @Transactional(readOnly = true)
    public DynamicTableOfContentsResponse generateDynamicToc(UUID projectId) {
        ResearchReport report = reportRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found for project: " + projectId));

        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId());
        List<ResearchReportSection> allSections = sectionRepository.findAllByReportIdOrderByChapterDisplayOrderAscDisplayOrderAsc(report.getId());

        Map<UUID, List<ResearchReportSection>> sectionsByChapterId = new HashMap<>();
        Map<UUID, List<ResearchReportSection>> subsectionsByParentId = new HashMap<>();

        for (ResearchReportSection sec : allSections) {
            if (sec.getParentSection() == null) {
                sectionsByChapterId.computeIfAbsent(sec.getChapter().getId(), k -> new ArrayList<>()).add(sec);
            } else {
                subsectionsByParentId.computeIfAbsent(sec.getParentSection().getId(), k -> new ArrayList<>()).add(sec);
            }
        }

        List<TocChapterItem> chapterItems = new ArrayList<>();
        int currentPage = 1;
        int totalSectionsCount = 0;

        for (ResearchReportChapter chapter : chapters) {
            int chapterStartPage = currentPage;
            List<ResearchReportSection> topSections = sectionsByChapterId.getOrDefault(chapter.getId(), List.of());
            topSections.sort(Comparator.comparingInt(ResearchReportSection::getDisplayOrder));

            List<TocSectionItem> sectionItems = new ArrayList<>();
            for (ResearchReportSection sec : topSections) {
                totalSectionsCount++;
                int sectionPage = currentPage;
                int words = countWords(sec.getContent());
                currentPage += Math.max(1, words / WORDS_PER_PAGE);

                List<TocSectionItem> subItems = buildSubsections(sec, subsectionsByParentId, currentPage);
                totalSectionsCount += countNestedSections(subItems);

                sectionItems.add(new TocSectionItem(
                        sec.getId(),
                        null,
                        sec.getSectionNumber(),
                        sec.getHeading(),
                        2,
                        sec.getDisplayOrder(),
                        sectionPage,
                        sec.getRequirementLevel(),
                        sec.getSemanticPurpose(),
                        subItems
                ));
            }

            chapterItems.add(new TocChapterItem(
                    chapter.getId(),
                    chapter.getChapterNumber(),
                    chapter.getTitle(),
                    chapter.getType(),
                    chapter.getDisplayOrder(),
                    chapterStartPage,
                    sectionItems
            ));

            // Chapter break: next chapter starts on fresh page
            currentPage++;
        }

        return new DynamicTableOfContentsResponse(
                report.getId(),
                report.getTitle(),
                chapterItems,
                chapters.size(),
                totalSectionsCount
        );
    }

    private List<TocSectionItem> buildSubsections(
            ResearchReportSection parent,
            Map<UUID, List<ResearchReportSection>> subsectionsByParentId,
            int parentPage
    ) {
        List<ResearchReportSection> children = subsectionsByParentId.getOrDefault(parent.getId(), List.of());
        if (children.isEmpty()) return List.of();

        children.sort(Comparator.comparingInt(ResearchReportSection::getDisplayOrder));
        List<TocSectionItem> items = new ArrayList<>();
        int subPage = parentPage;

        for (ResearchReportSection child : children) {
            int words = countWords(child.getContent());
            int childPage = subPage;
            subPage += Math.max(0, words / WORDS_PER_PAGE);

            List<TocSectionItem> deeper = buildSubsections(child, subsectionsByParentId, childPage);
            items.add(new TocSectionItem(
                    child.getId(),
                    parent.getId(),
                    child.getSectionNumber(),
                    child.getHeading(),
                    3,
                    child.getDisplayOrder(),
                    childPage,
                    child.getRequirementLevel(),
                    child.getSemanticPurpose(),
                    deeper
            ));
        }
        return items;
    }

    private int countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        return content.trim().split("\\s+").length;
    }

    private int countNestedSections(List<TocSectionItem> items) {
        if (items == null || items.isEmpty()) return 0;
        int count = items.size();
        for (TocSectionItem item : items) {
            count += countNestedSections(item.subsections());
        }
        return count;
    }
}
