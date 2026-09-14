package com.researchassistant.collaboration.service;

import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.analysis.repository.ResearchReportRepository;
import com.researchassistant.collaboration.dto.CollaborationDtos.CreateReportAuthorRequest;
import com.researchassistant.collaboration.dto.CollaborationDtos.ReportAuthorResponse;
import com.researchassistant.collaboration.entity.ResearchReportAuthor;
import com.researchassistant.collaboration.repository.ResearchReportAuthorRepository;
import com.researchassistant.common.exception.DuplicateResourceException;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.project.service.ProjectAuthorizationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ResearchReportAuthorService {
    private final ResearchReportAuthorRepository authorRepository;
    private final ResearchReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProjectAuthorizationService authorizationService;

    public ResearchReportAuthorService(ResearchReportAuthorRepository authorRepository, ResearchReportRepository reportRepository, UserRepository userRepository, ProjectAuthorizationService authorizationService) {
        this.authorRepository = authorRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    public ReportAuthorResponse add(UUID reportId, User actor, CreateReportAuthorRequest request) {
        ResearchReport report = reportRepository.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found."));
        authorizationService.requireProjectEditor(report.getProject().getId(), actor);
        if (authorRepository.existsByReportIdAndAuthorOrder(reportId, request.authorOrder())) {
            throw new DuplicateResourceException("Report author order is already used.");
        }
        ResearchReportAuthor author = new ResearchReportAuthor();
        author.setReport(report);
        author.setUser(request.userId() == null ? null : userRepository.findById(request.userId()).orElseThrow(() -> new ResourceNotFoundException("Author user not found.")));
        author.setDisplayName(request.displayName().trim());
        author.setStudentNumber(blankToNull(request.studentNumber()));
        author.setIndexNumber(blankToNull(request.indexNumber()));
        author.setProgramme(blankToNull(request.programme()));
        author.setAuthorOrder(request.authorOrder());
        author.setCorrespondingAuthor(request.correspondingAuthor());
        return toResponse(authorRepository.save(author));
    }

    @Transactional(readOnly = true)
    public List<ReportAuthorResponse> list(UUID reportId, User actor) {
        ResearchReport report = reportRepository.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found."));
        authorizationService.requireProjectViewer(report.getProject().getId(), actor);
        return authorRepository.findAllByReportIdOrderByAuthorOrderAsc(reportId).stream().map(this::toResponse).toList();
    }

    private ReportAuthorResponse toResponse(ResearchReportAuthor author) {
        return new ReportAuthorResponse(author.getId(), author.getReport().getId(), author.getUser() == null ? null : author.getUser().getId(), author.getDisplayName(), author.getStudentNumber(), author.getIndexNumber(), author.getProgramme(), author.getAuthorOrder(), author.isCorrespondingAuthor());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
