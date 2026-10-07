package com.researchassistant.analysis.service;

import com.researchassistant.analysis.entity.ResearchReport;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class FrontMatterRenderer {

    public String renderDeclaration(ResearchReport report) {
        String title = report.getTitle() != null ? report.getTitle() : "Project Report";
        String author = report.getAuthorName() != null ? report.getAuthorName() : "Author Name";
        String supervisor = report.getSupervisorName() != null ? report.getSupervisorName() : "Supervisor Name";
        String institution = report.getInstitutionName() != null ? report.getInstitutionName() : "the Institution";

        return "# DECLARATION\n\n"
                + "I hereby declare that this project report titled **\"" + title + "\"** is the result of my own original research, system design, and implementation work, conducted under the supervision of **" + supervisor + "**.\n\n"
                + "Where contributions of others or published literature have been drawn upon, they have been explicitly acknowledged and referenced in accordance with standard academic integrity rules. "
                + "No part of this report has been previously submitted in whole or in part for any degree or diploma at **" + institution + "** or any other institution of higher learning.\n\n"
                + "**Student / Researcher:** " + author + "  \n"
                + "**Signature:** ______________________________________  \n"
                + "**Date:** " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\n";
    }

    public String renderCertification(ResearchReport report) {
        String title = report.getTitle() != null ? report.getTitle() : "Project Report";
        String author = report.getAuthorName() != null ? report.getAuthorName() : "Author Name";
        String supervisor = report.getSupervisorName() != null ? report.getSupervisorName() : "Supervisor Name";
        String institution = report.getInstitutionName() != null ? report.getInstitutionName() : "the Institution";

        return "# CERTIFICATION\n\n"
                + "I hereby certify that the preparation and presentation of this project report titled **\"" + title + "\"** by **" + author + "** was conducted under my direct supervision in accordance with the project guidelines established by **" + institution + "**.\n\n"
                + "This report is deemed satisfactory and meets the academic requirements for submission.\n\n"
                + "**Supervisor:** " + supervisor + "  \n"
                + "**Signature:** ______________________________________  \n"
                + "**Date:** " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\n";
    }
}
