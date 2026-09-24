package com.researchassistant.reference.service;

import com.researchassistant.reference.dto.ReferenceDtos.AuthorRequest;
import com.researchassistant.reference.dto.ReferenceDtos.CreateReferenceRequest;
import com.researchassistant.reference.entity.*;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;

@Service
public class ReferenceInteroperabilityService {
    private final ReferenceNormalizationService normalizationService;

    public ReferenceInteroperabilityService(ReferenceNormalizationService normalizationService) {
        this.normalizationService = normalizationService;
    }

    public List<CreateReferenceRequest> parse(ReferenceImportFormat format, String content) {
        return switch (format) {
            case RIS -> parseRis(content);
            case BIBTEX -> parseBibtex(content);
            case ENDNOTE_XML -> parseEndnoteXml(content);
            case ENDNOTE_TAGGED -> throw new IllegalArgumentException("EndNote tagged import is modeled but not implemented.");
        };
    }

    public String export(ReferenceImportFormat format, List<ReferenceExportView> references) {
        return switch (format) {
            case RIS -> exportRis(references);
            case BIBTEX -> exportBibtex(references);
            case ENDNOTE_XML -> exportEndnoteXml(references);
            case ENDNOTE_TAGGED -> throw new IllegalArgumentException("EndNote tagged export is modeled but not implemented.");
        };
    }

    private List<CreateReferenceRequest> parseRis(String content) {
        List<CreateReferenceRequest> result = new ArrayList<>();
        Map<String, List<String>> current = new LinkedHashMap<>();
        for (String raw : content.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            if (raw.length() < 2) continue;
            String tag = raw.substring(0, 2);
            if (raw.length() >= 3 && raw.charAt(2) != ' ') continue;
            String value = raw.length() > 6 ? raw.substring(6).trim() : "";
            if ("TY".equals(tag)) current.clear();
            current.computeIfAbsent(tag, ignored -> new ArrayList<>()).add(value);
            if ("ER".equals(tag)) {
                result.add(fromRis(current));
                current = new LinkedHashMap<>();
            }
        }
        return result;
    }

    private CreateReferenceRequest fromRis(Map<String, List<String>> tags) {
        ReferenceType type = switch (first(tags, "TY", "GEN")) {
            case "JOUR" -> ReferenceType.JOURNAL_ARTICLE;
            case "BOOK" -> ReferenceType.BOOK;
            case "CHAP" -> ReferenceType.BOOK_CHAPTER;
            case "CONF" -> ReferenceType.CONFERENCE_PAPER;
            case "THES" -> ReferenceType.THESIS;
            case "RPRT" -> ReferenceType.REPORT;
            default -> ReferenceType.OTHER;
        };
        String pages = first(tags, "SP", null);
        if (pages != null && first(tags, "EP", null) != null) pages += "-" + first(tags, "EP", null);
        List<AuthorRequest> authors = tags.getOrDefault("AU", List.of()).stream().map(this::parseAuthor).toList();
        return new CreateReferenceRequest(type, first(tags, "TI", "Untitled reference"), first(tags, "JO", first(tags, "T2", null)),
                parseYear(first(tags, "PY", null)), first(tags, "VL", null), first(tags, "IS", null), pages,
                first(tags, "PB", null), null, null, null, null, null, null, first(tags, "DO", null),
                first(tags, "UR", null), first(tags, "SN", null), null, null, null, ReferenceMetadataStatus.PARTIAL,
                com.researchassistant.common.enums.ContentOrigin.IMPORTED, authors);
    }

    private List<CreateReferenceRequest> parseBibtex(String content) {
        List<CreateReferenceRequest> result = new ArrayList<>();
        int i = 0;
        while ((i = content.indexOf('@', i)) >= 0) {
            int open = content.indexOf('{', i);
            if (open < 0) break;
            String typeName = content.substring(i + 1, open).trim().toLowerCase(Locale.ROOT);
            int close = findMatchingBrace(content, open);
            if (close < 0) break;
            String body = content.substring(open + 1, close);
            int comma = body.indexOf(',');
            Map<String, String> fields = parseBibtexFields(comma < 0 ? body : body.substring(comma + 1));
            result.add(fromBibtex(typeName, fields));
            i = close + 1;
        }
        return result;
    }

    private CreateReferenceRequest fromBibtex(String typeName, Map<String, String> f) {
        ReferenceType type = switch (typeName) {
            case "article" -> ReferenceType.JOURNAL_ARTICLE;
            case "book" -> ReferenceType.BOOK;
            case "inproceedings" -> ReferenceType.CONFERENCE_PAPER;
            case "incollection" -> ReferenceType.BOOK_CHAPTER;
            case "phdthesis" -> ReferenceType.DISSERTATION;
            case "mastersthesis" -> ReferenceType.THESIS;
            case "techreport" -> ReferenceType.REPORT;
            default -> ReferenceType.OTHER;
        };
        List<AuthorRequest> authors = Arrays.stream(f.getOrDefault("author", "").split("\\s+and\\s+"))
                .filter(s -> !s.isBlank()).map(this::parseAuthor).toList();
        return new CreateReferenceRequest(type, f.getOrDefault("title", "Untitled reference"), firstNonNull(f.get("journal"), f.get("booktitle")),
                parseYear(f.get("year")), f.get("volume"), f.get("number"), f.get("pages"), f.get("publisher"),
                f.get("address"), f.get("edition"), f.get("institution"), f.get("booktitle"), null, f.get("language"),
                f.get("doi"), f.get("url"), f.get("isbn"), f.get("issn"), f.get("pmid"), f.get("eprint"),
                ReferenceMetadataStatus.PARTIAL, com.researchassistant.common.enums.ContentOrigin.IMPORTED, authors);
    }

    private List<CreateReferenceRequest> parseEndnoteXml(String content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(content)));
            NodeList records = doc.getElementsByTagName("record");
            List<CreateReferenceRequest> result = new ArrayList<>();
            for (int i = 0; i < records.getLength(); i++) {
                org.w3c.dom.Node record = records.item(i);
                result.add(new CreateReferenceRequest(ReferenceType.OTHER, text(record, "title", "Untitled reference"),
                        text(record, "secondary-title", null), parseYear(text(record, "year", null)), null, null,
                        text(record, "pages", null), text(record, "publisher", null), null, null, null, null, null,
                        null, text(record, "electronic-resource-num", null), text(record, "url", null), null, null,
                        null, null, ReferenceMetadataStatus.PARTIAL, com.researchassistant.common.enums.ContentOrigin.IMPORTED, List.of()));
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid or unsafe EndNote XML.");
        }
    }

    public String exportRis(List<ReferenceExportView> refs) {
        StringBuilder b = new StringBuilder();
        for (ReferenceExportView r : refs) {
            b.append("TY  - ").append(r.type() == ReferenceType.JOURNAL_ARTICLE ? "JOUR" : r.type() == ReferenceType.BOOK ? "BOOK" : "GEN").append('\n');
            for (String author : r.authors()) b.append("AU  - ").append(author).append('\n');
            if (r.year() != null) b.append("PY  - ").append(r.year()).append('\n');
            b.append("TI  - ").append(r.title()).append('\n');
            if (r.containerTitle() != null) b.append("JO  - ").append(r.containerTitle()).append('\n');
            if (r.volume() != null) b.append("VL  - ").append(r.volume()).append('\n');
            if (r.issue() != null) b.append("IS  - ").append(r.issue()).append('\n');
            if (r.pages() != null) {
                String[] pages = r.pages().split("-", 2);
                b.append("SP  - ").append(pages[0].trim()).append('\n');
                if (pages.length > 1) b.append("EP  - ").append(pages[1].trim()).append('\n');
            }
            if (r.publisher() != null) b.append("PB  - ").append(r.publisher()).append('\n');
            if (r.doi() != null) b.append("DO  - ").append(normalizationService.normalizeDoi(r.doi())).append('\n');
            if (r.url() != null) b.append("UR  - ").append(r.url()).append('\n');
            if (r.isbn() != null) b.append("SN  - ").append(r.isbn()).append('\n');
            if (r.issn() != null) b.append("SN  - ").append(r.issn()).append('\n');
            b.append("ER  - \n");
        }
        return b.toString();
    }

    public String exportBibtex(List<ReferenceExportView> refs) {
        StringBuilder b = new StringBuilder();
        int n = 1;
        for (ReferenceExportView r : refs) {
            b.append('@').append(r.type() == ReferenceType.JOURNAL_ARTICLE ? "article" : "misc").append("{ref").append(n++).append(",\n");
            b.append("  title = {").append(escape(r.title())).append("},\n");
            if (!r.authors().isEmpty()) b.append("  author = {").append(String.join(" and ", r.authors())).append("},\n");
            if (r.year() != null) b.append("  year = {").append(r.year()).append("},\n");
            if (r.containerTitle() != null) b.append("  journal = {").append(escape(r.containerTitle())).append("},\n");
            if (r.volume() != null) b.append("  volume = {").append(escape(r.volume())).append("},\n");
            if (r.issue() != null) b.append("  number = {").append(escape(r.issue())).append("},\n");
            if (r.pages() != null) b.append("  pages = {").append(escape(r.pages())).append("},\n");
            if (r.publisher() != null) b.append("  publisher = {").append(escape(r.publisher())).append("},\n");
            if (r.doi() != null) b.append("  doi = {").append(normalizationService.normalizeDoi(r.doi())).append("},\n");
            if (r.isbn() != null) b.append("  isbn = {").append(escape(r.isbn())).append("},\n");
            if (r.issn() != null) b.append("  issn = {").append(escape(r.issn())).append("},\n");
            b.append("}\n");
        }
        return b.toString();
    }

    public String exportEndnoteXml(List<ReferenceExportView> refs) {
        StringBuilder b = new StringBuilder("<xml><records>");
        for (ReferenceExportView r : refs) {
            b.append("<record><titles><title>").append(xml(r.title())).append("</title></titles>");
            if (!r.authors().isEmpty()) {
                b.append("<contributors><authors>");
                for (String author : r.authors()) b.append("<author>").append(xml(author)).append("</author>");
                b.append("</authors></contributors>");
            }
            if (r.year() != null) b.append("<dates><year>").append(r.year()).append("</year></dates>");
            if (r.containerTitle() != null) b.append("<periodical><full-title>").append(xml(r.containerTitle())).append("</full-title></periodical>");
            if (r.volume() != null || r.issue() != null || r.pages() != null) {
                b.append("<pages>").append(xml(r.pages())).append("</pages>");
                if (r.volume() != null) b.append("<volume>").append(xml(r.volume())).append("</volume>");
                if (r.issue() != null) b.append("<number>").append(xml(r.issue())).append("</number>");
            }
            if (r.publisher() != null) b.append("<publisher>").append(xml(r.publisher())).append("</publisher>");
            if (r.doi() != null) b.append("<electronic-resource-num>").append(xml(normalizationService.normalizeDoi(r.doi()))).append("</electronic-resource-num>");
            if (r.url() != null) b.append("<urls><related-urls><url>").append(xml(r.url())).append("</url></related-urls></urls>");
            b.append("</record>");
        }
        return b.append("</records></xml>").toString();
    }

    private Map<String, String> parseBibtexFields(String body) {
        Map<String, String> fields = new LinkedHashMap<>();
        int i = 0;
        while (i < body.length()) {
            while (i < body.length() && Character.isWhitespace(body.charAt(i)) || i < body.length() && body.charAt(i) == ',') i++;
            int eq = body.indexOf('=', i);
            if (eq < 0) break;
            String key = body.substring(i, eq).trim().toLowerCase(Locale.ROOT);
            i = eq + 1;
            while (i < body.length() && Character.isWhitespace(body.charAt(i))) i++;
            ParsedValue parsed = readBibtexValue(body, i);
            fields.put(key, parsed.value());
            i = parsed.nextIndex();
        }
        return fields;
    }

    private ParsedValue readBibtexValue(String body, int i) {
        if (i >= body.length()) return new ParsedValue("", i);
        char quote = body.charAt(i);
        if (quote == '{') {
            int end = findMatchingBrace(body, i);
            return new ParsedValue(stripOuter(body.substring(i, end + 1)), end + 1);
        }
        if (quote == '"') {
            int end = i + 1;
            while (end < body.length() && body.charAt(end) != '"') end++;
            return new ParsedValue(body.substring(i + 1, Math.min(end, body.length())), end + 1);
        }
        int end = i;
        while (end < body.length() && body.charAt(end) != ',') end++;
        return new ParsedValue(body.substring(i, end).trim(), end);
    }

    private int findMatchingBrace(String value, int open) {
        int depth = 0;
        for (int i = open; i < value.length(); i++) {
            if (value.charAt(i) == '{') depth++;
            if (value.charAt(i) == '}' && --depth == 0) return i;
        }
        return -1;
    }

    private AuthorRequest parseAuthor(String value) {
        if (value == null || value.isBlank()) return new AuthorRequest(null, null, null, null, AuthorRole.AUTHOR);
        String v = stripOuter(value.trim());
        if (v.contains(",")) {
            String[] p = v.split(",", 2);
            return new AuthorRequest(p[0].trim(), p[1].trim(), null, null, AuthorRole.AUTHOR);
        }
        String[] p = v.split("\\s+");
        if (p.length == 1) return new AuthorRequest(null, null, v, null, AuthorRole.ORGANIZATION);
        return new AuthorRequest(p[p.length - 1], String.join(" ", Arrays.copyOf(p, p.length - 1)), null, null, AuthorRole.AUTHOR);
    }

    private Integer parseYear(String value) {
        if (value == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})").matcher(value);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private String first(Map<String, List<String>> tags, String tag, String fallback) { return tags.getOrDefault(tag, List.of()).stream().findFirst().orElse(fallback); }
    private String firstNonNull(String a, String b) { return a == null ? b : a; }
    private String stripOuter(String value) { return value.replaceAll("^\\{+", "").replaceAll("\\}+$", "").trim(); }
    private String escape(String value) { return value == null ? "" : value.replace("}", "\\}"); }
    private String xml(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private String text(org.w3c.dom.Node node, String tag, String fallback) { NodeList list = ((org.w3c.dom.Element) node).getElementsByTagName(tag); return list.getLength() == 0 ? fallback : list.item(0).getTextContent(); }
    private record ParsedValue(String value, int nextIndex) {}
    public record ReferenceExportView(ReferenceType type, String title, String containerTitle, Integer year,
                                      String volume, String issue, String pages, String publisher,
                                      String doi, String url, String isbn, String issn, List<String> authors) {}
}
