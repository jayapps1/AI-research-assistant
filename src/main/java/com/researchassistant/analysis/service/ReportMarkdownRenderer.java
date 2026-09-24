package com.researchassistant.analysis.service;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
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
import org.commonmark.node.ListBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ReportMarkdownRenderer {

    private static final Pattern RAW_HEADING = Pattern.compile("^\\s{0,3}#{1,6}\\s+.+$");
    private final Parser parser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

    public String normalizeSectionMarkdown(String heading, String markdown) {
        if (markdown == null) {
            return "";
        }
        String text = markdown
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("[citation metadata incomplete]", "")
                .replace("[Citation metadata incomplete]", "")
                .trim();
        if (text.isBlank()) {
            return "";
        }
        String[] lines = text.split("\n", -1);
        int start = 0;
        while (start < lines.length && lines[start].isBlank()) {
            start++;
        }
        while (start < lines.length && repeatsHeading(lines[start], heading)) {
            start++;
            while (start < lines.length && lines[start].isBlank()) {
                start++;
            }
        }
        StringBuilder normalized = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            normalized.append(lines[i]);
            if (i + 1 < lines.length) normalized.append('\n');
        }
        return normalized.toString().trim();
    }

    public boolean containsRawMarkdownHeading(String text) {
        if (text == null) return false;
        for (String line : text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            if (RAW_HEADING.matcher(line).matches()) return true;
        }
        return false;
    }

    public void renderMarkdown(XWPFDocument document, String markdown, String duplicateHeading) {
        String normalized = normalizeSectionMarkdown(duplicateHeading, markdown);
        if (normalized.isBlank()) {
            return;
        }
        RenderState state = new RenderState(document);
        Node parsed = parser.parse(normalized);
        for (Node child = parsed.getFirstChild(); child != null; child = child.getNext()) {
            renderBlock(document, child, state, 0);
        }
    }

    public List<String> plainLines(String markdown, String duplicateHeading) {
        String normalized = normalizeSectionMarkdown(duplicateHeading, markdown);
        if (normalized.isBlank()) return List.of();
        Node parsed = parser.parse(normalized);
        List<String> lines = new ArrayList<>();
        for (Node child = parsed.getFirstChild(); child != null; child = child.getNext()) {
            renderPlainBlock(child, lines, 0, 1);
        }
        return lines.stream().filter(line -> line != null && !line.isBlank()).toList();
    }

    private void renderBlock(XWPFDocument document, Node node, RenderState state, int listLevel) {
        if (node instanceof Heading heading) {
            XWPFParagraph paragraph = createParagraph(document, ParagraphAlignment.LEFT);
            paragraph.setStyle("Heading" + Math.min(Math.max(heading.getLevel(), 1), 4));
            XWPFRun run = run(paragraph, true, false);
            run.setFontSize(heading.getLevel() == 1 ? 14 : 13);
            run.setText(literalText(heading));
            return;
        }
        if (node instanceof Paragraph paragraphNode) {
            XWPFParagraph paragraph = createParagraph(document, ParagraphAlignment.BOTH);
            renderInlines(paragraph, paragraphNode.getFirstChild(), false, false);
            return;
        }
        if (node instanceof BulletList list) {
            renderList(document, list, state, listLevel, false);
            return;
        }
        if (node instanceof OrderedList list) {
            renderList(document, list, state, listLevel, true);
            return;
        }
        if (node instanceof BlockQuote quote) {
            for (Node child = quote.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof Paragraph paragraphNode) {
                    XWPFParagraph paragraph = createParagraph(document, ParagraphAlignment.BOTH);
                    paragraph.setIndentationLeft(720);
                    renderInlines(paragraph, paragraphNode.getFirstChild(), false, true);
                } else {
                    renderBlock(document, child, state, listLevel);
                }
            }
            return;
        }
        if (node instanceof FencedCodeBlock codeBlock) {
            codeParagraph(document, codeBlock.getLiteral());
            return;
        }
        if (node instanceof IndentedCodeBlock codeBlock) {
            codeParagraph(document, codeBlock.getLiteral());
            return;
        }
        if (node instanceof TableBlock tableBlock) {
            renderTable(document, tableBlock);
            return;
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            renderBlock(document, child, state, listLevel);
        }
    }

    private void renderList(XWPFDocument document, ListBlock list, RenderState state, int level, boolean ordered) {
        BigInteger numId = ordered ? state.orderedNumId() : state.bulletNumId();
        for (Node itemNode = list.getFirstChild(); itemNode != null; itemNode = itemNode.getNext()) {
            if (!(itemNode instanceof ListItem item)) {
                continue;
            }
            for (Node child = item.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof Paragraph paragraphNode) {
                    XWPFParagraph paragraph = createParagraph(document, ParagraphAlignment.BOTH);
                    paragraph.setStyle("ListParagraph");
                    paragraph.setNumID(numId);
                    paragraph.setNumILvl(BigInteger.valueOf(Math.max(level, 0)));
                    renderInlines(paragraph, paragraphNode.getFirstChild(), false, false);
                } else if (child instanceof BulletList nested) {
                    renderList(document, nested, state, level + 1, false);
                } else if (child instanceof OrderedList nested) {
                    renderList(document, nested, state, level + 1, true);
                } else {
                    renderBlock(document, child, state, level);
                }
            }
        }
    }

    private void renderTable(XWPFDocument document, TableBlock tableBlock) {
        List<List<String>> rows = new ArrayList<>();
        for (Node child = tableBlock.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof TableHead || child instanceof TableBody) {
                for (Node row = child.getFirstChild(); row != null; row = row.getNext()) {
                    if (row instanceof TableRow tableRow) {
                        rows.add(tableCells(tableRow));
                    }
                }
            }
        }
        if (rows.isEmpty()) return;
        int columns = rows.stream().mapToInt(List::size).max().orElse(1);
        XWPFTable table = document.createTable(rows.size(), columns);
        table.setWidth("100%");
        for (int r = 0; r < rows.size(); r++) {
            for (int c = 0; c < columns; c++) {
                String value = c < rows.get(r).size() ? rows.get(r).get(c) : "";
                table.getRow(r).getCell(c).setText(value);
            }
        }
    }

    private List<String> tableCells(TableRow row) {
        List<String> cells = new ArrayList<>();
        for (Node cell = row.getFirstChild(); cell != null; cell = cell.getNext()) {
            if (cell instanceof TableCell tableCell) {
                cells.add(literalText(tableCell).trim());
            }
        }
        return cells;
    }

    private void renderInlines(XWPFParagraph paragraph, Node node, boolean bold, boolean italic) {
        for (Node child = node; child != null; child = child.getNext()) {
            if (child instanceof Text text) {
                XWPFRun run = run(paragraph, bold, italic);
                run.setText(text.getLiteral());
            } else if (child instanceof Code code) {
                XWPFRun run = run(paragraph, bold, italic);
                run.setFontFamily("Courier New");
                run.setText(code.getLiteral());
            } else if (child instanceof StrongEmphasis) {
                renderInlines(paragraph, child.getFirstChild(), true, italic);
            } else if (child instanceof Emphasis) {
                renderInlines(paragraph, child.getFirstChild(), bold, true);
            } else if (child instanceof Link) {
                renderInlines(paragraph, child.getFirstChild(), bold, italic);
            } else if (child instanceof SoftLineBreak) {
                XWPFRun run = run(paragraph, bold, italic);
                run.setText(" ");
            } else if (child instanceof HardLineBreak) {
                run(paragraph, bold, italic).addBreak();
            } else {
                renderInlines(paragraph, child.getFirstChild(), bold, italic);
            }
        }
    }

    private void renderPlainBlock(Node node, List<String> lines, int listLevel, int orderedIndex) {
        if (node instanceof Heading heading) {
            lines.add(literalText(heading));
            return;
        }
        if (node instanceof Paragraph paragraph) {
            lines.add(literalText(paragraph));
            return;
        }
        if (node instanceof BulletList || node instanceof OrderedList) {
            boolean ordered = node instanceof OrderedList;
            int index = 1;
            for (Node item = node.getFirstChild(); item != null; item = item.getNext()) {
                String prefix = " ".repeat(Math.max(0, listLevel * 2)) + (ordered ? (index++) + ". " : "- ");
                Node first = item.getFirstChild();
                if (first instanceof Paragraph paragraph) {
                    lines.add(prefix + literalText(paragraph));
                    for (Node child = first.getNext(); child != null; child = child.getNext()) {
                        renderPlainBlock(child, lines, listLevel + 1, 1);
                    }
                } else {
                    renderPlainBlock(item, lines, listLevel + 1, 1);
                }
            }
            return;
        }
        if (node instanceof TableBlock tableBlock) {
            for (List<String> row : plainTableRows(tableBlock)) {
                lines.add(String.join("    ", row));
            }
            return;
        }
        if (node instanceof FencedCodeBlock codeBlock) {
            lines.add(codeBlock.getLiteral());
            return;
        }
        if (node instanceof IndentedCodeBlock codeBlock) {
            lines.add(codeBlock.getLiteral());
            return;
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            renderPlainBlock(child, lines, listLevel, orderedIndex);
        }
    }

    private List<List<String>> plainTableRows(TableBlock tableBlock) {
        List<List<String>> rows = new ArrayList<>();
        for (Node child = tableBlock.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof TableHead || child instanceof TableBody) {
                for (Node row = child.getFirstChild(); row != null; row = row.getNext()) {
                    if (row instanceof TableRow tableRow) rows.add(tableCells(tableRow));
                }
            }
        }
        return rows;
    }

    private boolean repeatsHeading(String line, String heading) {
        if (heading == null || heading.isBlank() || line == null) return false;
        String candidate = line.trim().replaceFirst("^#{1,6}\\s+", "").trim();
        return normalizeHeading(candidate).equals(normalizeHeading(heading));
    }

    private String normalizeHeading(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private XWPFParagraph createParagraph(XWPFDocument document, ParagraphAlignment alignment) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        paragraph.setStyle("Normal");
        paragraph.setSpacingAfter(120);
        paragraph.setSpacingBetween(1.5);
        return paragraph;
    }

    private XWPFRun run(XWPFParagraph paragraph, boolean bold, boolean italic) {
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Times New Roman");
        run.setFontSize(12);
        run.setBold(bold);
        run.setItalic(italic);
        return run;
    }

    private void codeParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = createParagraph(document, ParagraphAlignment.LEFT);
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Courier New");
        run.setFontSize(10);
        run.setText(text == null ? "" : text);
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

    private static final class RenderState {
        private final XWPFDocument document;
        private BigInteger bulletNumId;
        private BigInteger orderedNumId;

        private RenderState(XWPFDocument document) {
            this.document = document;
        }

        private BigInteger bulletNumId() {
            if (bulletNumId == null) bulletNumId = createNumId(false);
            return bulletNumId;
        }

        private BigInteger orderedNumId() {
            if (orderedNumId == null) orderedNumId = createNumId(true);
            return orderedNumId;
        }

        private BigInteger createNumId(boolean ordered) {
            XWPFNumbering numbering = document.getNumbering() == null ? document.createNumbering() : document.getNumbering();
            BigInteger abstractId = BigInteger.valueOf(System.nanoTime()).abs();
            CTAbstractNum abstractNum = CTAbstractNum.Factory.newInstance();
            abstractNum.setAbstractNumId(abstractId);
            for (int level = 0; level < 4; level++) {
                CTLvl lvl = abstractNum.addNewLvl();
                lvl.setIlvl(BigInteger.valueOf(level));
                lvl.addNewStart().setVal(BigInteger.ONE);
                lvl.addNewNumFmt().setVal(ordered ? STNumberFormat.DECIMAL : STNumberFormat.BULLET);
                lvl.addNewLvlText().setVal(ordered ? "%" + (level + 1) + "." : "\u2022");
                CTInd indent = lvl.addNewPPr().addNewInd();
                indent.setLeft(BigInteger.valueOf(720L * (level + 1)));
                indent.setHanging(BigInteger.valueOf(360L));
            }
            BigInteger id = numbering.addAbstractNum(new XWPFAbstractNum(abstractNum));
            return numbering.addNum(id);
        }
    }
}
