package com.researchassistant.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReportRichTextService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Parser parser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

    public String markdownToDocumentJson(String markdown) {
        ObjectNode doc = docNode();
        ArrayNode content = doc.putArray("content");
        String normalized = normalize(markdown);
        if (normalized.isBlank()) {
            content.add(paragraphNode(""));
            return write(doc);
        }
        Node parsed = parser.parse(normalized);
        for (Node child = parsed.getFirstChild(); child != null; child = child.getNext()) {
            appendBlock(content, child);
        }
        if (content.isEmpty()) {
            content.add(paragraphNode(""));
        }
        return write(doc);
    }

    public String plainTextFromMarkdown(String markdown) {
        return plainTextFromDocumentJson(markdownToDocumentJson(markdown));
    }

    public String plainTextFromDocumentJson(String contentJson) {
        try {
            JsonNode root = objectMapper.readTree(contentJson);
            StringBuilder out = new StringBuilder();
            appendPlain(root, out);
            return out.toString().replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    public String documentJsonToMarkdown(String contentJson) {
        try {
            JsonNode root = objectMapper.readTree(contentJson);
            StringBuilder out = new StringBuilder();
            JsonNode content = root.get("content");
            if (content != null && content.isArray()) {
                for (JsonNode node : content) {
                    appendMarkdownBlock(node, out, 0);
                }
            }
            return out.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    public boolean isValidDocumentJson(String contentJson) {
        try {
            JsonNode node = objectMapper.readTree(contentJson);
            return node != null && "doc".equals(text(node.get("type")));
        } catch (Exception ignored) {
            return false;
        }
    }

    public String assembleDocumentJson(List<DocumentPart> parts) {
        ObjectNode doc = docNode();
        ArrayNode content = doc.putArray("content");
        for (DocumentPart part : parts) {
            if (part.heading() != null && !part.heading().isBlank()) {
                content.add(headingNode(part.heading(), part.level()));
            }
            if (part.contentJson() == null || part.contentJson().isBlank()) {
                continue;
            }
            try {
                JsonNode parsed = objectMapper.readTree(part.contentJson());
                JsonNode partContent = parsed.get("content");
                if (partContent != null && partContent.isArray()) {
                    for (JsonNode node : partContent) {
                        content.add(node.deepCopy());
                    }
                }
            } catch (Exception ignored) {
                content.add(paragraphNode(part.plainText() == null ? "" : part.plainText()));
            }
        }
        if (content.isEmpty()) {
            content.add(paragraphNode(""));
        }
        return write(doc);
    }

    private void appendBlock(ArrayNode target, Node node) {
        if (node instanceof Heading heading) {
            target.add(headingNode(literalText(heading), heading.getLevel()));
            return;
        }
        if (node instanceof Paragraph paragraph) {
            ObjectNode p = paragraphNode(null);
            appendInline(p.putArray("content"), paragraph.getFirstChild());
            if (!p.has("content") || p.get("content").isEmpty()) {
                p.remove("content");
            }
            target.add(p);
            return;
        }
        if (node instanceof BulletList list) {
            ObjectNode listNode = typed("bulletList");
            ArrayNode items = listNode.putArray("content");
            appendListItems(items, list, false);
            target.add(listNode);
            return;
        }
        if (node instanceof OrderedList list) {
            ObjectNode listNode = typed("orderedList");
            ObjectNode attrs = listNode.putObject("attrs");
            attrs.put("start", list.getStartNumber());
            ArrayNode items = listNode.putArray("content");
            appendListItems(items, list, true);
            target.add(listNode);
            return;
        }
        if (node instanceof BlockQuote quote) {
            ObjectNode quoteNode = typed("blockquote");
            ArrayNode content = quoteNode.putArray("content");
            for (Node child = quote.getFirstChild(); child != null; child = child.getNext()) {
                appendBlock(content, child);
            }
            target.add(quoteNode);
            return;
        }
        if (node instanceof FencedCodeBlock codeBlock) {
            target.add(codeBlockNode(codeBlock.getLiteral()));
            return;
        }
        if (node instanceof IndentedCodeBlock codeBlock) {
            target.add(codeBlockNode(codeBlock.getLiteral()));
            return;
        }
        if (node instanceof TableBlock table) {
            target.add(tableNode(table));
            return;
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            appendBlock(target, child);
        }
    }

    private void appendListItems(ArrayNode items, Node list, boolean ordered) {
        for (Node item = list.getFirstChild(); item != null; item = item.getNext()) {
            if (!(item instanceof ListItem)) {
                continue;
            }
            ObjectNode itemNode = typed("listItem");
            ArrayNode itemContent = itemNode.putArray("content");
            for (Node child = item.getFirstChild(); child != null; child = child.getNext()) {
                appendBlock(itemContent, child);
            }
            if (itemContent.isEmpty()) {
                itemContent.add(paragraphNode(""));
            }
            items.add(itemNode);
        }
    }

    private void appendInline(ArrayNode target, Node node) {
        appendInline(target, node, null);
    }

    private void appendInline(ArrayNode target, Node node, ArrayNode inheritedMarks) {
        for (Node child = node; child != null; child = child.getNext()) {
            if (child instanceof Text text) {
                target.add(textNode(text.getLiteral(), inheritedMarks));
            } else if (child instanceof Code code) {
                ArrayNode marks = copyMarks(inheritedMarks);
                marks.add(mark("code"));
                target.add(textNode(code.getLiteral(), marks));
            } else if (child instanceof StrongEmphasis) {
                ArrayNode marks = copyMarks(inheritedMarks);
                marks.add(mark("bold"));
                appendInline(target, child.getFirstChild(), marks);
            } else if (child instanceof Emphasis) {
                ArrayNode marks = copyMarks(inheritedMarks);
                marks.add(mark("italic"));
                appendInline(target, child.getFirstChild(), marks);
            } else if (child instanceof Link link) {
                ArrayNode marks = copyMarks(inheritedMarks);
                ObjectNode linkMark = mark("link");
                linkMark.putObject("attrs").put("href", link.getDestination());
                marks.add(linkMark);
                appendInline(target, child.getFirstChild(), marks);
            } else if (child instanceof SoftLineBreak) {
                target.add(textNode(" ", inheritedMarks));
            } else if (child instanceof HardLineBreak) {
                target.add(typed("hardBreak"));
            } else {
                appendInline(target, child.getFirstChild(), inheritedMarks);
            }
        }
    }

    private ObjectNode tableNode(TableBlock tableBlock) {
        ObjectNode table = typed("table");
        ArrayNode rows = table.putArray("content");
        for (Node child = tableBlock.getFirstChild(); child != null; child = child.getNext()) {
            boolean header = child instanceof TableHead;
            if (child instanceof TableHead || child instanceof TableBody) {
                for (Node row = child.getFirstChild(); row != null; row = row.getNext()) {
                    if (row instanceof TableRow tableRow) {
                        rows.add(tableRowNode(tableRow, header));
                    }
                }
            }
        }
        return table;
    }

    private ObjectNode tableRowNode(TableRow row, boolean header) {
        ObjectNode rowNode = typed("tableRow");
        ArrayNode cells = rowNode.putArray("content");
        for (Node cell = row.getFirstChild(); cell != null; cell = cell.getNext()) {
            if (cell instanceof TableCell tableCell) {
                ObjectNode cellNode = typed(header ? "tableHeader" : "tableCell");
                ArrayNode cellContent = cellNode.putArray("content");
                ObjectNode p = paragraphNode(null);
                appendInline(p.putArray("content"), tableCell.getFirstChild());
                cellContent.add(p);
                cells.add(cellNode);
            }
        }
        return rowNode;
    }

    private void appendPlain(JsonNode node, StringBuilder out) {
        if (node == null) return;
        if (node.has("text")) {
            out.append(node.get("text").asText());
        }
        String type = text(node.get("type"));
        if ("hardBreak".equals(type)) {
            out.append('\n');
        }
        JsonNode content = node.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode child : content) appendPlain(child, out);
        }
        if (isBlock(type)) {
            out.append('\n');
        }
    }

    private void appendMarkdownBlock(JsonNode node, StringBuilder out, int listLevel) {
        String type = text(node.get("type"));
        if ("heading".equals(type)) {
            int level = node.path("attrs").path("level").asInt(1);
            out.append("#".repeat(Math.max(1, Math.min(6, level)))).append(' ').append(inlineMarkdown(node)).append("\n\n");
            return;
        }
        if ("paragraph".equals(type)) {
            String value = inlineMarkdown(node);
            if (!value.isBlank()) out.append(value).append("\n\n");
            return;
        }
        if ("bulletList".equals(type) || "orderedList".equals(type)) {
            boolean ordered = "orderedList".equals(type);
            int index = Math.max(1, node.path("attrs").path("start").asInt(1));
            for (JsonNode item : node.path("content")) {
                String prefix = "  ".repeat(Math.max(0, listLevel)) + (ordered ? (index++) + ". " : "- ");
                out.append(prefix).append(firstParagraphMarkdown(item)).append('\n');
                for (JsonNode child : item.path("content")) {
                    String childType = text(child.get("type"));
                    if ("bulletList".equals(childType) || "orderedList".equals(childType)) {
                        appendMarkdownBlock(child, out, listLevel + 1);
                    }
                }
            }
            out.append('\n');
            return;
        }
        if ("blockquote".equals(type)) {
            for (String line : inlineOrChildMarkdown(node).split("\\R")) {
                if (!line.isBlank()) out.append("> ").append(line).append('\n');
            }
            out.append('\n');
            return;
        }
        if ("codeBlock".equals(type)) {
            out.append("```\n").append(inlineMarkdown(node)).append("\n```\n\n");
            return;
        }
        if ("table".equals(type)) {
            appendMarkdownTable(node, out);
            return;
        }
        for (JsonNode child : node.path("content")) {
            appendMarkdownBlock(child, out, listLevel);
        }
    }

    private String firstParagraphMarkdown(JsonNode item) {
        for (JsonNode child : item.path("content")) {
            if ("paragraph".equals(text(child.get("type")))) {
                return inlineMarkdown(child);
            }
        }
        return inlineMarkdown(item);
    }

    private String inlineOrChildMarkdown(JsonNode node) {
        String own = inlineMarkdown(node);
        if (!own.isBlank()) return own;
        StringBuilder b = new StringBuilder();
        for (JsonNode child : node.path("content")) appendMarkdownBlock(child, b, 0);
        return b.toString().trim();
    }

    private String inlineMarkdown(JsonNode node) {
        StringBuilder out = new StringBuilder();
        appendInlineMarkdown(node.path("content"), out);
        return out.toString().trim();
    }

    private void appendInlineMarkdown(JsonNode content, StringBuilder out) {
        if (content == null || !content.isArray()) return;
        for (JsonNode child : content) {
            String type = text(child.get("type"));
            if ("text".equals(type)) {
                String value = child.path("text").asText("");
                boolean bold = hasMark(child, "bold");
                boolean italic = hasMark(child, "italic");
                if (bold) value = "**" + value + "**";
                if (italic) value = "*" + value + "*";
                out.append(value);
            } else if ("hardBreak".equals(type)) {
                out.append('\n');
            } else if ("citation".equals(type)) {
                out.append("[[citation:")
                        .append(child.path("attrs").path("referenceId").asText(""))
                        .append("]]");
            } else {
                appendInlineMarkdown(child.path("content"), out);
            }
        }
    }

    private boolean hasMark(JsonNode node, String markType) {
        for (JsonNode mark : node.path("marks")) {
            if (markType.equals(text(mark.get("type")))) return true;
        }
        return false;
    }

    private void appendMarkdownTable(JsonNode table, StringBuilder out) {
        boolean wroteSeparator = false;
        for (JsonNode row : table.path("content")) {
            List<String> cells = new java.util.ArrayList<>();
            boolean header = false;
            for (JsonNode cell : row.path("content")) {
                header = header || "tableHeader".equals(text(cell.get("type")));
                cells.add(inlineOrChildMarkdown(cell).replace("\n", " "));
            }
            out.append("| ").append(String.join(" | ", cells)).append(" |\n");
            if (header && !wroteSeparator) {
                out.append("| ").append(String.join(" | ", cells.stream().map(ignored -> "---").toList())).append(" |\n");
                wroteSeparator = true;
            }
        }
        out.append('\n');
    }

    private ObjectNode docNode() {
        return typed("doc");
    }

    private ObjectNode paragraphNode(String text) {
        ObjectNode node = typed("paragraph");
        if (text != null && !text.isEmpty()) {
            node.putArray("content").add(textNode(text, null));
        }
        return node;
    }

    private ObjectNode headingNode(String text, int level) {
        ObjectNode node = typed("heading");
        node.putObject("attrs").put("level", Math.max(1, Math.min(6, level)));
        if (text != null && !text.isBlank()) {
            node.putArray("content").add(textNode(text, null));
        }
        return node;
    }

    private ObjectNode codeBlockNode(String text) {
        ObjectNode node = typed("codeBlock");
        if (text != null && !text.isEmpty()) {
            node.putArray("content").add(textNode(text, null));
        }
        return node;
    }

    private ObjectNode textNode(String value, ArrayNode marks) {
        ObjectNode node = typed("text");
        node.put("text", value == null ? "" : value);
        if (marks != null && !marks.isEmpty()) {
            node.set("marks", marks.deepCopy());
        }
        return node;
    }

    private ObjectNode mark(String type) {
        return typed(type);
    }

    private ObjectNode typed(String type) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", type);
        return node;
    }

    private ArrayNode copyMarks(ArrayNode marks) {
        return marks == null ? objectMapper.createArrayNode() : marks.deepCopy();
    }

    private String literalText(Node node) {
        StringBuilder text = new StringBuilder();
        appendLiteral(node.getFirstChild(), text);
        return text.toString().replaceAll("\\s+", " ").trim();
    }

    private void appendLiteral(Node node, StringBuilder text) {
        for (Node child = node; child != null; child = child.getNext()) {
            if (child instanceof Text literal) {
                text.append(literal.getLiteral());
            } else if (child instanceof Code code) {
                text.append(code.getLiteral());
            } else if (child instanceof SoftLineBreak || child instanceof HardLineBreak) {
                text.append(' ');
            } else {
                appendLiteral(child.getFirstChild(), text);
            }
        }
    }

    private boolean isBlock(String type) {
        return type != null && switch (type) {
            case "paragraph", "heading", "listItem", "blockquote", "codeBlock", "tableRow" -> true;
            default -> false;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to serialize report rich text.");
        }
    }

    public record DocumentPart(String heading, int level, String contentJson, String plainText) {}
}
