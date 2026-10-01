package com.cms.ai.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits extracted document text into paragraph-sized chunks before embedding (RAG ingestion,
 * OC-277). Groups adjacent paragraphs up to {@link #MAX_CHUNK_CHARS}; a paragraph longer than
 * that on its own is hard-split into fixed windows -- this is the common path in practice, since
 * PDFTextStripper emits a newline per line of text, not per real paragraph, so a whole page often
 * arrives as one "paragraph" with no blank-line breaks.
 */
public final class DocumentChunker {

    static final int MAX_CHUNK_CHARS = 1000;

    private DocumentChunker() {
    }

    public static List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        StringBuilder current = new StringBuilder();
        for (String rawParagraph : text.split("\\n\\s*\\n")) {
            String paragraph = rawParagraph.strip();
            if (paragraph.isEmpty()) {
                continue;
            }
            for (String piece : splitIfTooLong(paragraph)) {
                if (current.length() > 0 && current.length() + piece.length() + 1 > MAX_CHUNK_CHARS) {
                    chunks.add(current.toString());
                    current.setLength(0);
                }
                if (current.length() > 0) {
                    current.append('\n');
                }
                current.append(piece);
                if (current.length() >= MAX_CHUNK_CHARS) {
                    chunks.add(current.toString());
                    current.setLength(0);
                }
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private static List<String> splitIfTooLong(String paragraph) {
        if (paragraph.length() <= MAX_CHUNK_CHARS) {
            return List.of(paragraph);
        }
        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + MAX_CHUNK_CHARS, paragraph.length());
            pieces.add(paragraph.substring(start, end));
            start = end;
        }
        return pieces;
    }
}
