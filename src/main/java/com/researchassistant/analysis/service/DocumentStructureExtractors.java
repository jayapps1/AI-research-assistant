package com.researchassistant.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.analysis.entity.ResearchReportSection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DocumentStructureExtractors {

    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^\\)]+)\\)");
    private static final Pattern FIGURE_CAPTION_PATTERN = Pattern.compile("(?i)^figure\\s+(\\d+(?:\\.\\d+)*)[:\\s]+(.*)$", Pattern.MULTILINE);
    private static final Pattern TABLE_CAPTION_PATTERN = Pattern.compile("(?i)^table\\s+(\\d+(?:\\.\\d+)*)[:\\s]+(.*)$", Pattern.MULTILINE);
    private static final Pattern MD_TABLE_PATTERN = Pattern.compile("\\|([^\\n\\r]+)\\|\\r?\\n\\|[-:\\s|]+\\|");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public record FigureEntry(String number, String caption, String sectionHeading) {}
    public record TableEntry(String number, String caption, String sectionHeading) {}

    public List<FigureEntry> extractFigures(List<ResearchReportSection> sections) {
        List<FigureEntry> figures = new ArrayList<>();
        int defaultFigureIndex = 1;

        for (ResearchReportSection section : sections) {
            String content = section.getContent();
            String contentJson = section.getContentJson();

            // 1. Check markdown image patterns
            if (content != null) {
                Matcher imgMatcher = MD_IMAGE_PATTERN.matcher(content);
                while (imgMatcher.find()) {
                    String alt = imgMatcher.group(1).trim();
                    String figNum = section.getSectionNumber() != null ? section.getSectionNumber() + "." + (figures.size() + 1) : "Figure " + defaultFigureIndex++;
                    String caption = !alt.isBlank() ? alt : "Illustration for " + section.getHeading();
                    figures.add(new FigureEntry(figNum, caption, section.getHeading()));
                }

                // 2. Check explicit figure caption lines
                Matcher capMatcher = FIGURE_CAPTION_PATTERN.matcher(content);
                while (capMatcher.find()) {
                    String num = "Figure " + capMatcher.group(1).trim();
                    String text = capMatcher.group(2).trim();
                    boolean exists = figures.stream().anyMatch(f -> f.caption().equalsIgnoreCase(text));
                    if (!exists) {
                        figures.add(new FigureEntry(num, text, section.getHeading()));
                    }
                }
            }

            // 3. Inspect JSON nodes if figures list still empty for this section
            if (contentJson != null && (contentJson.contains("\"type\":\"image\"") || contentJson.contains("\"type\":\"figure\""))) {
                try {
                    JsonNode root = objectMapper.readTree(contentJson);
                    collectJsonImages(root, section, figures);
                } catch (Exception ignored) {}
            }
        }
        return figures;
    }

    public List<TableEntry> extractTables(List<ResearchReportSection> sections) {
        List<TableEntry> tables = new ArrayList<>();
        int defaultTableIndex = 1;

        for (ResearchReportSection section : sections) {
            String content = section.getContent();
            String contentJson = section.getContentJson();

            if (content != null) {
                // 1. Explicit table caption lines
                Matcher capMatcher = TABLE_CAPTION_PATTERN.matcher(content);
                while (capMatcher.find()) {
                    String num = "Table " + capMatcher.group(1).trim();
                    String text = capMatcher.group(2).trim();
                    tables.add(new TableEntry(num, text, section.getHeading()));
                }

                // 2. Detect markdown tables if not already captured by caption
                Matcher tableMatcher = MD_TABLE_PATTERN.matcher(content);
                while (tableMatcher.find()) {
                    String headerRow = tableMatcher.group(1).trim();
                    String tabNum = section.getSectionNumber() != null
                            ? "Table " + section.getSectionNumber() + "." + (tables.size() + 1)
                            : "Table " + defaultTableIndex++;
                    String caption = "Summary Data Table (" + headerRow.replace("|", ", ") + ")";
                    boolean exists = tables.stream().anyMatch(t -> t.sectionHeading().equals(section.getHeading()));
                    if (!exists) {
                        tables.add(new TableEntry(tabNum, caption, section.getHeading()));
                    }
                }
            }

            if (contentJson != null && contentJson.contains("\"type\":\"table\"") && tables.stream().noneMatch(t -> t.sectionHeading().equals(section.getHeading()))) {
                String tabNum = section.getSectionNumber() != null
                        ? "Table " + section.getSectionNumber() + ".1"
                        : "Table " + defaultTableIndex++;
                tables.add(new TableEntry(tabNum, "Data Table for " + section.getHeading(), section.getHeading()));
            }
        }
        return tables;
    }

    private void collectJsonImages(JsonNode node, ResearchReportSection section, List<FigureEntry> figures) {
        if (node == null || node.isNull()) return;
        String type = node.path("type").asText();
        if ("image".equals(type) || "figure".equals(type)) {
            String title = node.path("attrs").path("title").asText("");
            String captionAttr = node.path("attrs").path("caption").asText("");
            String label = node.path("attrs").path("figureLabel").asText("");
            String alt = node.path("attrs").path("alt").asText("");
            String captionCandidate = !captionAttr.isBlank()
                    ? captionAttr
                    : (!title.isBlank() ? title : (!alt.isBlank() ? alt : "Figure for " + section.getHeading()));
            String caption = captionCandidate;
            String num = !label.isBlank()
                    ? label
                    : (section.getSectionNumber() != null ? "Figure " + section.getSectionNumber() + "." + (figures.size() + 1) : "Figure " + (figures.size() + 1));
            boolean exists = figures.stream().anyMatch(f -> f.caption().equalsIgnoreCase(caption));
            if (!exists) {
                figures.add(new FigureEntry(num, caption, section.getHeading()));
            }
        }
        JsonNode content = node.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode child : content) {
                collectJsonImages(child, section, figures);
            }
        }
    }

    public String renderListOfFiguresMarkdown(List<FigureEntry> figures) {
        StringBuilder sb = new StringBuilder();
        sb.append("# LIST OF FIGURES\n\n");
        if (figures.isEmpty()) {
            sb.append("*No figures have been included in this report.*\n");
            return sb.toString();
        }
        for (FigureEntry f : figures) {
            sb.append("- **").append(f.number()).append(":** ").append(f.caption())
              .append(" *(in ").append(f.sectionHeading()).append(")*\n");
        }
        return sb.toString();
    }

    public String renderListOfTablesMarkdown(List<TableEntry> tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("# LIST OF TABLES\n\n");
        if (tables.isEmpty()) {
            sb.append("*No tables have been included in this report.*\n");
            return sb.toString();
        }
        for (TableEntry t : tables) {
            sb.append("- **").append(t.number()).append(":** ").append(t.caption())
              .append(" *(in ").append(t.sectionHeading()).append(")*\n");
        }
        return sb.toString();
    }

    public String renderFiguresMarkdown(List<FigureEntry> figures) {
        return renderListOfFiguresMarkdown(figures);
    }

    public String renderTablesMarkdown(List<TableEntry> tables) {
        return renderListOfTablesMarkdown(tables);
    }
}
