package com.researchassistant.document.chunk;

import com.researchassistant.document.config.DocumentProperties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicTextChunkerTests {

    @Test
    void chunkingIsDeterministicPageLocalAndUsesValidOffsets() {
        DeterministicTextChunker chunker = chunker(60, 10, 20);
        String page = """
                Alpha sentence one. Alpha sentence two.

                Beta sentence one. Beta sentence two.

                Gamma sentence one. Gamma sentence two.
                """.trim();

        List<ChunkSlice> first = chunker.chunk(page);
        List<ChunkSlice> second = chunker.chunk(page);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSizeGreaterThan(1);
        for (ChunkSlice chunk : first) {
            assertThat(chunk.startInclusive()).isGreaterThanOrEqualTo(0);
            assertThat(chunk.endExclusive()).isGreaterThan(chunk.startInclusive());
            assertThat(page.substring(chunk.startInclusive(), chunk.endExclusive()).trim())
                    .isEqualTo(chunk.text());
            assertThat(chunk.text()).isNotBlank();
        }
    }

    @Test
    void overlapIsAppliedBetweenAdjacentChunks() {
        DeterministicTextChunker chunker = chunker(40, 8, 10);
        String page = "a ".repeat(80).trim();

        List<ChunkSlice> chunks = chunker.chunk(page);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(1).startInclusive())
                .isLessThan(chunks.get(0).endExclusive());
    }

    @Test
    void largeParagraphIsSplitAtHardLimitWithoutEmptyChunks() {
        DeterministicTextChunker chunker = chunker(25, 5, 10);
        String page = "x".repeat(90);

        List<ChunkSlice> chunks = chunker.chunk(page);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.text()).isNotBlank();
            assertThat(chunk.endExclusive() - chunk.startInclusive())
                    .isLessThanOrEqualTo(25);
        });
    }

    private DeterministicTextChunker chunker(
            int target,
            int overlap,
            int minimum
    ) {
        return new DeterministicTextChunker(new DocumentProperties(
                1000,
                new DocumentProperties.Storage("local", "./target/test"),
                new DocumentProperties.Chunking(target, overlap, minimum),
                new DocumentProperties.Processing(false)
        ));
    }
}
