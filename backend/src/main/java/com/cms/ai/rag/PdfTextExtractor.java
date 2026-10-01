package com.cms.ai.rag;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Extracts selectable text from a digitally-native PDF for the RAG document-ingestion pipeline
 * (OC-277). v1 scope only -- scanned/image PDFs with no real text layer come back as an empty
 * string (not an error); OCR for that case is an explicit phase 2.
 */
public final class PdfTextExtractor {

    private PdfTextExtractor() {
    }

    /**
     * @throws IOException if the file cannot be read as a PDF at all (corrupt, encrypted
     *                      without a password, or not actually a PDF) -- callers treat this as
     *                      a permanent failure for that document, distinct from the empty-text
     *                      case below which is expected and not an error.
     */
    public static String extractText(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
