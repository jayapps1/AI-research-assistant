package com.researchassistant.analysis.service;

import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.project.entity.ResearchProject;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
public class TitlePageRenderer {

    public record TitlePageData(
            String title,
            String authorName,
            String studentId,
            String institutionName,
            String departmentName,
            String degreeProgram,
            String supervisorName,
            String academicYear,
            Integer submissionYear
    ) {}

    public TitlePageData resolveData(ResearchReport report) {
        ResearchProject project = report.getProject();
        String title = report.getTitle() != null && !report.getTitle().isBlank()
                ? report.getTitle().trim()
                : (project != null && project.getTitle() != null ? project.getTitle().trim() : "Project Title");

        String author = report.getAuthorName() != null && !report.getAuthorName().isBlank()
                ? report.getAuthorName().trim()
                : (project != null && project.getCreatedBy() != null ? project.getCreatedBy().getFullName() : "Student Name");

        String inst = report.getInstitutionName() != null && !report.getInstitutionName().isBlank()
                ? report.getInstitutionName().trim()
                : (project != null && project.getInstitution() != null && !project.getInstitution().isBlank() ? project.getInstitution().trim() : "Institution Name");

        String dept = report.getDepartmentName() != null && !report.getDepartmentName().isBlank()
                ? report.getDepartmentName().trim()
                : (project != null && project.getDepartment() != null && !project.getDepartment().isBlank() ? project.getDepartment().trim() : "Department of Computer Science");

        String prog = report.getDegreeProgram() != null && !report.getDegreeProgram().isBlank()
                ? report.getDegreeProgram().trim()
                : (project != null && project.getProgramme() != null && !project.getProgramme().isBlank() ? project.getProgramme().trim() : "Bachelor of Science in Computer Science");

        String sup = report.getSupervisorName() != null && !report.getSupervisorName().isBlank()
                ? report.getSupervisorName().trim()
                : (project != null && project.getSupervisor() != null && !project.getSupervisor().isBlank() ? project.getSupervisor().trim() : "Project Supervisor");

        String acadYear = project != null && project.getAcademicYear() != null && !project.getAcademicYear().isBlank()
                ? project.getAcademicYear().trim()
                : Year.now().toString();

        Integer subYear = report.getSubmissionYear() != null
                ? report.getSubmissionYear()
                : Year.now().getValue();

        return new TitlePageData(
                title,
                author,
                null,
                inst,
                dept,
                prog,
                sup,
                acadYear,
                subYear
        );
    }

    public String renderMarkdown(TitlePageData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(data.title().toUpperCase()).append("\n\n");
        sb.append("A Project Report Submitted to the  \n");
        sb.append("**").append(data.departmentName()).append("**  \n");
        sb.append("**").append(data.institutionName()).append("**  \n\n");
        sb.append("In Partial Fulfillment of the Requirements for the Award of the Degree of  \n");
        sb.append("**").append(data.degreeProgram()).append("**  \n\n");
        sb.append("By  \n");
        sb.append("**").append(data.authorName()).append("**  \n");
        if (data.studentId() != null && !data.studentId().isBlank()) {
            sb.append("*Student ID / Index No:* ").append(data.studentId()).append("  \n");
        }
        sb.append("\n");
        sb.append("**Supervisor:** ").append(data.supervisorName()).append("  \n");
        if (data.academicYear() != null && !data.academicYear().isBlank()) {
            sb.append("**Academic Year:** ").append(data.academicYear()).append("  \n");
        }
        sb.append("**Submission Year:** ").append(data.submissionYear() != null ? data.submissionYear() : Year.now().getValue()).append("\n");
        return sb.toString();
    }
}
