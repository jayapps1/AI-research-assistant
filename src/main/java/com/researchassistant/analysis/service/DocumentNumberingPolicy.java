package com.researchassistant.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.analysis.entity.ResearchReportTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DocumentNumberingPolicy {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final NumberStyle chapterNumberStyle;
    private final NumberStyle sectionNumberStyle;
    private final NumberStyle subsectionNumberStyle;
    private final boolean chapterWordPrefix;

    private DocumentNumberingPolicy(NumberStyle chapterNumberStyle, NumberStyle sectionNumberStyle,
            NumberStyle subsectionNumberStyle, boolean chapterWordPrefix) {
        this.chapterNumberStyle = chapterNumberStyle;
        this.sectionNumberStyle = sectionNumberStyle;
        this.subsectionNumberStyle = subsectionNumberStyle;
        this.chapterWordPrefix = chapterWordPrefix;
    }

    public static DocumentNumberingPolicy fromReport(ResearchReport report) {
        ResearchReportTemplate template = report == null ? null : report.getTemplate();
        return fromTemplate(template);
    }

    public static DocumentNumberingPolicy fromTemplate(ResearchReportTemplate template) {
        NumberStyle chapterStyle = NumberStyle.UPPER_WORD;
        NumberStyle sectionStyle = NumberStyle.ARABIC;
        NumberStyle subsectionStyle = NumberStyle.ARABIC;
        boolean prefix = true;
        if (template == null || template.getConfigurationJson() == null || template.getConfigurationJson().isBlank()) {
            return new DocumentNumberingPolicy(chapterStyle, sectionStyle, subsectionStyle, prefix);
        }
        try {
            JsonNode root = MAPPER.readTree(template.getConfigurationJson());
            JsonNode fp = root.path("formatProfile");
            JsonNode numbering = fp.path("numbering");
            if (numbering.isObject()) {
                chapterStyle = NumberStyle.parse(numbering.path("chapterNumberStyle").asText(null), chapterStyle);
                sectionStyle = NumberStyle.parse(numbering.path("sectionNumberStyle").asText(null), sectionStyle);
                subsectionStyle = NumberStyle.parse(numbering.path("subsectionNumberStyle").asText(null), subsectionStyle);
                prefix = numbering.path("chapterWordPrefix").asBoolean(prefix);
            } else {
                String headingNumbering = fp.path("headingNumbering").asText("");
                if ("CHAPTER_ROMAN".equalsIgnoreCase(headingNumbering)) {
                    chapterStyle = NumberStyle.UPPER_ROMAN;
                } else if ("DECIMAL".equalsIgnoreCase(headingNumbering) || "CHAPTER_DECIMAL".equalsIgnoreCase(headingNumbering)) {
                    chapterStyle = NumberStyle.ARABIC;
                }
            }
        } catch (Exception ignored) {
            return new DocumentNumberingPolicy(chapterStyle, sectionStyle, subsectionStyle, prefix);
        }
        return new DocumentNumberingPolicy(chapterStyle, sectionStyle, subsectionStyle, prefix);
    }

    public String chapterHeading(Integer chapterNumber, String title) {
        String cleanTitle = title == null ? "" : title.trim();
        if (chapterNumber == null) {
            return cleanTitle;
        }
        String number = chapterNumberStyle.format(chapterNumber);
        if (chapterWordPrefix) {
            return ("CHAPTER " + number + (cleanTitle.isBlank() ? "" : " - " + cleanTitle)).trim();
        }
        return (number + (cleanTitle.isBlank() ? "" : ". " + cleanTitle)).trim();
    }

    public String sectionNumber(String storedNumber) {
        if (storedNumber == null || storedNumber.isBlank()) {
            return "";
        }
        String[] parts = storedNumber.split("\\.");
        List<String> formatted = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            int value;
            try {
                value = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ignored) {
                formatted.add(parts[i]);
                continue;
            }
            NumberStyle style = i <= 1 ? sectionNumberStyle : subsectionNumberStyle;
            formatted.add(style.format(value));
        }
        return String.join(".", formatted);
    }

    public String sectionHeading(String storedNumber, String heading) {
        String number = sectionNumber(storedNumber);
        String cleanHeading = heading == null ? "" : heading.trim();
        return number.isBlank() ? cleanHeading : number + " " + cleanHeading;
    }

    public enum NumberStyle {
        ARABIC,
        UPPER_ROMAN,
        LOWER_ROMAN,
        UPPER_ALPHA,
        LOWER_ALPHA,
        UPPER_WORD,
        LOWER_WORD;

        static NumberStyle parse(String value, NumberStyle fallback) {
            if (value == null || value.isBlank()) {
                return fallback;
            }
            String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            if ("ROMAN_UPPER".equals(normalized)) normalized = "UPPER_ROMAN";
            if ("ROMAN_LOWER".equals(normalized)) normalized = "LOWER_ROMAN";
            if ("ALPHA_UPPER".equals(normalized)) normalized = "UPPER_ALPHA";
            if ("ALPHA_LOWER".equals(normalized)) normalized = "LOWER_ALPHA";
            if ("WORD_UPPER".equals(normalized)) normalized = "UPPER_WORD";
            if ("WORD_LOWER".equals(normalized)) normalized = "LOWER_WORD";
            try {
                return NumberStyle.valueOf(normalized);
            } catch (Exception ignored) {
                return fallback;
            }
        }

        String format(int value) {
            return switch (this) {
                case ARABIC -> String.valueOf(value);
                case UPPER_ROMAN -> roman(value);
                case LOWER_ROMAN -> roman(value).toLowerCase(Locale.ROOT);
                case UPPER_ALPHA -> alpha(value).toUpperCase(Locale.ROOT);
                case LOWER_ALPHA -> alpha(value).toLowerCase(Locale.ROOT);
                case UPPER_WORD -> word(value).toUpperCase(Locale.ROOT);
                case LOWER_WORD -> word(value).toLowerCase(Locale.ROOT);
            };
        }

        private static String roman(int value) {
            if (value <= 0 || value > 3999) return String.valueOf(value);
            int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
            String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
            StringBuilder out = new StringBuilder();
            int remaining = value;
            for (int i = 0; i < values.length; i++) {
                while (remaining >= values[i]) {
                    out.append(numerals[i]);
                    remaining -= values[i];
                }
            }
            return out.toString();
        }

        private static String alpha(int value) {
            if (value <= 0) return String.valueOf(value);
            StringBuilder out = new StringBuilder();
            int current = value;
            while (current > 0) {
                current--;
                out.insert(0, (char) ('A' + current % 26));
                current /= 26;
            }
            return out.toString();
        }

        private static String word(int value) {
            return switch (value) {
                case 1 -> "One";
                case 2 -> "Two";
                case 3 -> "Three";
                case 4 -> "Four";
                case 5 -> "Five";
                case 6 -> "Six";
                case 7 -> "Seven";
                case 8 -> "Eight";
                case 9 -> "Nine";
                case 10 -> "Ten";
                case 11 -> "Eleven";
                case 12 -> "Twelve";
                case 13 -> "Thirteen";
                case 14 -> "Fourteen";
                case 15 -> "Fifteen";
                case 16 -> "Sixteen";
                case 17 -> "Seventeen";
                case 18 -> "Eighteen";
                case 19 -> "Nineteen";
                case 20 -> "Twenty";
                default -> String.valueOf(value);
            };
        }
    }
}
