package com.cms.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class DocumentChunkerTest {

    @Test
    void nullText_returnsNoChunks() {
        assertThat(DocumentChunker.chunk(null)).isEmpty();
    }

    @Test
    void blankText_returnsNoChunks() {
        assertThat(DocumentChunker.chunk("   \n\n  \n")).isEmpty();
    }

    @Test
    void singleShortParagraph_returnsOneChunkUnchanged() {
        String text = "Transfer certificate for Jane Doe.";
        assertThat(DocumentChunker.chunk(text)).containsExactly(text);
    }

    @Test
    void multipleShortParagraphs_mergeIntoOneChunk() {
        String text = "Paragraph one.\n\nParagraph two.\n\nParagraph three.";
        List<String> chunks = DocumentChunker.chunk(text);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("Paragraph one.", "Paragraph two.", "Paragraph three.");
    }

    @Test
    void paragraphsExceedingMaxChunkSize_splitAcrossMultipleChunks() {
        String paragraph = "x".repeat(600);
        // three ~600-char paragraphs: first two merge (1200 > 1000 after two, so they must
        // split before the third), proving chunk boundaries respect MAX_CHUNK_CHARS.
        String text = paragraph + "\n\n" + paragraph + "\n\n" + paragraph;

        List<String> chunks = DocumentChunker.chunk(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        chunks.forEach(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(DocumentChunker.MAX_CHUNK_CHARS));
    }

    @Test
    void singleParagraphLongerThanMax_hardSplitIntoFixedWindows() {
        // no blank-line breaks at all -- the common real case, since PDFTextStripper emits a
        // newline per line of text, not per real paragraph.
        String text = "y".repeat(2500);

        List<String> chunks = DocumentChunker.chunk(text);

        assertThat(chunks).hasSize(3); // 1000 + 1000 + 500
        assertThat(chunks.get(0)).hasSize(1000);
        assertThat(chunks.get(1)).hasSize(1000);
        assertThat(chunks.get(2)).hasSize(500);
        assertThat(String.join("", chunks)).hasSize(2500);
    }

    @Test
    void paragraphExactlyAtMaxChunkSize_formsExactlyOneChunk() {
        String text = "z".repeat(DocumentChunker.MAX_CHUNK_CHARS);

        List<String> chunks = DocumentChunker.chunk(text);

        assertThat(chunks).containsExactly(text);
    }

    @Test
    void extraBlankLinesBetweenParagraphs_stillSplitCorrectly() {
        String text = "First.\n\n\n\n\nSecond.";
        List<String> chunks = DocumentChunker.chunk(text);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("First.\nSecond.");
    }
}
