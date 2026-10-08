package com.researchassistant.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.common.storage.ObjectStorageService;
import com.researchassistant.common.storage.StorageObjectCategory;
import com.researchassistant.common.storage.StorageObjectEntity;
import com.researchassistant.common.storage.StorageObjectMetadataService;
import com.researchassistant.common.storage.StoredObject;
import com.researchassistant.evidence.entity.EvidenceAnalysisStatus;
import com.researchassistant.evidence.entity.EvidenceType;
import com.researchassistant.evidence.entity.ProjectEvidence;
import com.researchassistant.evidence.repository.ProjectEvidenceRepository;
import com.researchassistant.evidence.service.ProjectEvidenceNumberingService;
import com.researchassistant.identity.entity.User;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AcademicGeneratedFigureService {

    private static final Pattern FIGURE_BLOCK = Pattern.compile("(?s)```(?:academic_figure|generated_figure|section_figure)\\s*(\\{.*?})\\s*```");
    private static final int MAX_DEFINITION_CHARS = 8000;

    private final ProjectEvidenceRepository evidenceRepository;
    private final ProjectEvidenceNumberingService numberingService;
    private final StorageObjectMetadataService storageObjectMetadataService;
    private final ObjectStorageService objectStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AcademicGeneratedFigureService(
            ProjectEvidenceRepository evidenceRepository,
            ProjectEvidenceNumberingService numberingService,
            StorageObjectMetadataService storageObjectMetadataService,
            ObjectStorageService objectStorageService
    ) {
        this.evidenceRepository = evidenceRepository;
        this.numberingService = numberingService;
        this.storageObjectMetadataService = storageObjectMetadataService;
        this.objectStorageService = objectStorageService;
    }

    public FigureProcessingResult process(ResearchReportSection section, String markdown, User user) {
        if (section == null || markdown == null || markdown.isBlank() || !markdown.contains("```")) {
            return new FigureProcessingResult(markdown == null ? "" : markdown, Map.of());
        }
        Matcher matcher = FIGURE_BLOCK.matcher(markdown);
        StringBuffer out = new StringBuffer();
        Map<UUID, ReportRichTextService.FigureRenderData> renderData = new LinkedHashMap<>();
        while (matcher.find()) {
            GeneratedFigureSpec spec = parseSpec(matcher.group(1));
            if (spec == null || isUnsupportedEmpiricalFigure(spec, section)) {
                matcher.appendReplacement(out, "");
                continue;
            }
            ProjectEvidence figure = createFigure(section, spec, user);
            Map<UUID, String> labels = numberingService.computeDynamicLabelsForReport(section.getChapter().getReport().getId());
            String label = numberingService.resolveLabel(figure, labels);
            figure.setFigureLabel(label);
            renderData.put(figure.getId(), new ReportRichTextService.FigureRenderData(
                    figure.getId(),
                    label,
                    figure.getCaption(),
                    figure.getStorageObject() != null ? "/api/v1/storage-objects/" + figure.getStorageObject().getId() + "/download" : "",
                    figure.getAltText(),
                    figure.getStructuredDefinition(),
                    "MERMAID"
            ));
            matcher.appendReplacement(out, Matcher.quoteReplacement(figurePlaceholderWithAcademicScaffold(figure.getId(), spec)));
        }
        matcher.appendTail(out);
        return new FigureProcessingResult(out.toString().trim(), renderData);
    }

    private GeneratedFigureSpec parseSpec(String rawJson) {
        try {
            JsonNode node = objectMapper.readTree(rawJson);
            String figureType = clean(node.path("figureType").asText(node.path("type").asText("DIAGRAM")));
            String title = clean(node.path("title").asText(""));
            String caption = clean(node.path("caption").asText(title));
            String definition = cleanDefinition(node.path("definition").asText(node.path("structuredDefinition").asText("")));
            String description = clean(node.path("description").asText(""));
            List<String> evidenceIds = new ArrayList<>();
            JsonNode evidence = node.path("evidenceIds");
            if (evidence.isArray()) {
                evidence.forEach(item -> evidenceIds.add(item.asText()));
            }
            if (caption.isBlank() || definition.isBlank()) {
                return null;
            }
            return new GeneratedFigureSpec(figureType, title.isBlank() ? caption : title, caption, definition, description, evidenceIds);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isUnsupportedEmpiricalFigure(GeneratedFigureSpec spec, ResearchReportSection section) {
        String type = spec.figureType().toUpperCase(Locale.ROOT);
        boolean empirical = type.contains("CHART") || type.contains("PLOT") || type.contains("HEATMAP")
                || type.contains("BENCHMARK") || type.contains("RESULT") || type.contains("STATISTICAL");
        if (!empirical) {
            return false;
        }
        return evidenceRepository.findAllBySectionIdOrderByDisplayOrderAscCreatedAtAsc(section.getId()).stream()
                .noneMatch(e -> e.getEvidenceType() == EvidenceType.DATASET_RESULT
                        || e.getEvidenceType() == EvidenceType.TEST_RESULT
                        || e.getEvidenceType() == EvidenceType.CHART
                        || e.getEvidenceType() == EvidenceType.TABLE);
    }

    private ProjectEvidence createFigure(ResearchReportSection section, GeneratedFigureSpec spec, User user) {
        UUID figureId = UUID.randomUUID();
        byte[] svgBytes = renderSimpleSvg(spec).getBytes(StandardCharsets.UTF_8);
        String filename = safeFilename(spec.title()) + ".svg";
        String storageKey = "projects/" + section.getChapter().getReport().getProject().getId()
                + "/evidence/" + figureId + "/" + filename;

        StorageObjectEntity pending = storageObjectMetadataService.createPending(
                user.getId(),
                section.getChapter().getReport().getProject().getWorkspace().getId(),
                section.getChapter().getReport().getProject().getId(),
                StorageObjectCategory.IMAGE,
                storageKey,
                filename,
                filename,
                "image/svg+xml",
                svgBytes.length,
                null
        );
        StorageObjectEntity available;
        try {
            StoredObject stored = objectStorageService.store(storageKey, new ByteArrayInputStream(svgBytes), "image/svg+xml");
            available = storageObjectMetadataService.markAvailable(pending.getId(), stored);
        } catch (RuntimeException ex) {
            storageObjectMetadataService.markFailed(pending.getId(), "FIGURE_RENDER_FAILED", ex.getMessage());
            throw ex;
        }

        ProjectEvidence figure = new ProjectEvidence();
        figure.setId(figureId);
        figure.setProject(section.getChapter().getReport().getProject());
        figure.setWorkspace(section.getChapter().getReport().getProject().getWorkspace());
        figure.setReport(section.getChapter().getReport());
        figure.setSection(section);
        figure.setStorageObject(available);
        figure.setStorageKey(storageKey);
        figure.setOriginalFilename(filename);
        figure.setMimeType("image/svg+xml");
        figure.setFileSizeBytes(svgBytes.length);
        figure.setEvidenceType(toEvidenceType(spec.figureType()));
        figure.setCaption(spec.caption());
        figure.setDescription(spec.description().isBlank() ? "AI-generated academic synthesis figure." : spec.description());
        figure.setAltText(spec.caption());
        figure.setDisplayOrder((int) evidenceRepository.countByProjectId(figure.getProject().getId()) + 1);
        figure.setCreatedBy(user);
        figure.setAiAnalysisStatus(EvidenceAnalysisStatus.NOT_ANALYZED);
        figure.setSourceType("AI_SYNTHESIS");
        figure.setGenerationSource("SECTION_GENERATION");
        figure.setStructuredDefinition(spec.definition());
        figure.setEvidenceSourceIds(toJson(spec.evidenceIds()));
        figure.setMetadataJson(metadataJson(spec));
        return evidenceRepository.save(figure);
    }

    private EvidenceType toEvidenceType(String figureType) {
        String type = figureType == null ? "" : figureType.toUpperCase(Locale.ROOT);
        if (type.contains("ARCHITECTURE")) return EvidenceType.ARCHITECTURE_DIAGRAM;
        if (type.contains("USE_CASE")) return EvidenceType.USE_CASE_DIAGRAM;
        if (type.contains("ERD") || type.contains("ENTITY")) return EvidenceType.ER_DIAGRAM;
        if (type.contains("FLOW")) return EvidenceType.FLOWCHART;
        if (type.contains("CHART") || type.contains("PLOT") || type.contains("HEATMAP")) return EvidenceType.CHART;
        return EvidenceType.DIAGRAM;
    }

    private String figurePlaceholderWithAcademicScaffold(UUID figureId, GeneratedFigureSpec spec) {
        StringBuilder out = new StringBuilder();
        out.append("\n\n");
        out.append("The relationships discussed above are summarized in [[figure-ref:")
                .append(figureId)
                .append("]].\n\n");
        out.append("[[figure:").append(figureId).append("]]\n\n");
        out.append("[[figure-ref:")
                .append(figureId)
                .append("]] shows how the selected concepts or system elements relate to one another within this section's argument.\n\n");
        return out.toString();
    }

    private String renderSimpleSvg(GeneratedFigureSpec spec) {
        List<String> labels = extractDiagramLabels(spec.definition());
        if (labels.isEmpty()) {
            labels = List.of(spec.title(), "Supported concepts", "Academic interpretation");
        }
        int boxWidth = 210;
        int boxHeight = 58;
        int gap = 28;
        int width = Math.max(760, labels.size() * (boxWidth + gap) + 80);
        int height = 260;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width).append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>");
        svg.append("<text x=\"").append(width / 2).append("\" y=\"34\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"18\" font-weight=\"700\" fill=\"#111827\">")
                .append(escapeXml(spec.title())).append("</text>");
        int y = 96;
        int x = 40;
        for (int i = 0; i < labels.size(); i++) {
            svg.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(boxWidth).append("\" height=\"").append(boxHeight).append("\" rx=\"6\" fill=\"#eef2ff\" stroke=\"#475569\"/>");
            svg.append("<text x=\"").append(x + boxWidth / 2).append("\" y=\"").append(y + 34).append("\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"13\" fill=\"#111827\">")
                    .append(escapeXml(shortText(labels.get(i), 28))).append("</text>");
            if (i + 1 < labels.size()) {
                int x1 = x + boxWidth;
                int x2 = x + boxWidth + gap;
                svg.append("<line x1=\"").append(x1).append("\" y1=\"").append(y + boxHeight / 2).append("\" x2=\"").append(x2).append("\" y2=\"").append(y + boxHeight / 2).append("\" stroke=\"#334155\" stroke-width=\"2\" marker-end=\"url(#arrow)\"/>");
            }
            x += boxWidth + gap;
        }
        svg.append("<defs><marker id=\"arrow\" markerWidth=\"8\" markerHeight=\"8\" refX=\"8\" refY=\"4\" orient=\"auto\"><path d=\"M0,0 L8,4 L0,8 z\" fill=\"#334155\"/></marker></defs>");
        svg.append("<text x=\"").append(width / 2).append("\" y=\"220\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"12\" fill=\"#475569\">")
                .append(escapeXml(spec.caption())).append("</text>");
        svg.append("</svg>");
        return svg.toString();
    }

    private List<String> extractDiagramLabels(String definition) {
        List<String> labels = new ArrayList<>();
        Matcher bracketed = Pattern.compile("\\[([^\\[\\]]{2,80})]").matcher(definition);
        while (bracketed.find() && labels.size() < 4) {
            labels.add(bracketed.group(1).trim());
        }
        if (!labels.isEmpty()) return labels;
        for (String line : definition.split("\\R")) {
            String cleaned = line.replaceAll("[-=]+>", " ").replaceAll("[();{}]", " ").replaceAll("\\s+", " ").trim();
            if (!cleaned.isBlank() && !cleaned.toLowerCase(Locale.ROOT).startsWith("flowchart") && labels.size() < 4) {
                labels.add(shortText(cleaned, 40));
            }
        }
        return labels;
    }

    private String metadataJson(GeneratedFigureSpec spec) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("definitionFormat", "MERMAID");
        node.put("figureType", spec.figureType());
        node.put("sourceType", "AI_SYNTHESIS");
        node.put("generationSource", "SECTION_GENERATION");
        node.put("structuredDefinition", spec.definition());
        node.set("evidenceIds", objectMapper.valueToTree(spec.evidenceIds()));
        return node.toString();
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private String cleanDefinition(String value) {
        String clean = clean(value).replaceAll("(?i)<script.*?</script>", "");
        if (clean.length() > MAX_DEFINITION_CHARS) {
            clean = clean.substring(0, MAX_DEFINITION_CHARS);
        }
        return clean;
    }

    private String safeFilename(String value) {
        String clean = value == null ? "generated-figure" : value.trim().toLowerCase(Locale.ROOT);
        clean = clean.replaceAll("[^a-z0-9._-]+", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        return clean.isBlank() ? "generated-figure" : clean;
    }

    private String shortText(String value, int max) {
        if (value == null) return "";
        String clean = value.replaceAll("\\s+", " ").trim();
        return clean.length() <= max ? clean : clean.substring(0, Math.max(0, max - 1)).trim() + "...";
    }

    private String escapeXml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public record FigureProcessingResult(String markdown, Map<UUID, ReportRichTextService.FigureRenderData> figures) {}

    private record GeneratedFigureSpec(String figureType, String title, String caption, String definition,
                                       String description, List<String> evidenceIds) {}
}
