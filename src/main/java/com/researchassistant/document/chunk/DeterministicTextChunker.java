package com.researchassistant.document.chunk;

import com.researchassistant.document.config.DocumentProperties;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DeterministicTextChunker {

    private final int targetCharacters;
    private final int overlapCharacters;
    private final int minimumCharacters;

    public DeterministicTextChunker(DocumentProperties properties) {
        this.targetCharacters = properties.chunking().targetCharacters();
        this.overlapCharacters = properties.chunking().overlapCharacters();
        this.minimumCharacters = properties.chunking().minimumCharacters();
    }

    public List<ChunkSlice> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int target = Math.max(1, targetCharacters);
        int overlap = Math.max(0, Math.min(overlapCharacters, target / 2));
        int minimum = Math.max(1, Math.min(minimumCharacters, target));

        List<ChunkSlice> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + target);
            if (end < text.length()) {
                end = bestSplit(text, start, end, minimum);
            }
            String chunkText = text.substring(start, end).trim();
            int adjustedStart = start + leadingWhitespace(text, start, end);
            int adjustedEnd = end - trailingWhitespace(text, start, end);
            if (!chunkText.isBlank() && adjustedEnd > adjustedStart) {
                chunks.add(new ChunkSlice(chunkText, adjustedStart, adjustedEnd));
            }
            if (end >= text.length()) {
                break;
            }
            int nextStart = Math.max(0, end - overlap);
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }
        return chunks;
    }

    private int bestSplit(String text, int start, int hardEnd, int minimum) {
        int paragraph = text.lastIndexOf("\n\n", hardEnd);
        if (paragraph >= start + minimum) {
            return paragraph;
        }
        int sentence = Math.max(
                Math.max(text.lastIndexOf(". ", hardEnd), text.lastIndexOf("? ", hardEnd)),
                text.lastIndexOf("! ", hardEnd)
        );
        if (sentence >= start + minimum) {
            return sentence + 1;
        }
        int newline = text.lastIndexOf('\n', hardEnd);
        if (newline >= start + minimum) {
            return newline;
        }
        int space = text.lastIndexOf(' ', hardEnd);
        if (space >= start + minimum) {
            return space;
        }
        return hardEnd;
    }

    private int leadingWhitespace(String text, int start, int end) {
        int count = 0;
        for (int i = start; i < end && Character.isWhitespace(text.charAt(i)); i++) {
            count++;
        }
        return count;
    }

    private int trailingWhitespace(String text, int start, int end) {
        int count = 0;
        for (int i = end - 1; i >= start && Character.isWhitespace(text.charAt(i)); i--) {
            count++;
        }
        return count;
    }
}
