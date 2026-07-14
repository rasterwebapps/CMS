package com.cms.util.export;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Shared OpenPDF rendering pieces reused by every {@code *ExportService} — landscape
 * document setup, the title/filter/sort metadata block, and the header table builder —
 * so each service only owns its own per-DTO data-row extraction and column layout.
 */
public final class PdfExportUtil {

    private PdfExportUtil() {}

    public static final Color HEADER_BG = new Color(13, 27, 62);
    public static final Color ALT_BG = new Color(235, 241, 255);

    public static Document openLandscapeDocument(ByteArrayOutputStream out,
                                                  float left, float right, float top, float bottom) throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), left, right, top, bottom);
        PdfWriter.getInstance(doc, out);
        doc.open();
        return doc;
    }

    /** Writes the title paragraph, then one italic grey line per active filter, then a sort line. */
    public static void writeTitleAndMetadata(Document doc, ExportMetadata meta) throws IOException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(90, 90, 90));

        boolean hasMeta = !meta.filters().isEmpty() || meta.sortLabel() != null;

        Paragraph title = new Paragraph(meta.title(), titleFont);
        title.setSpacingAfter(hasMeta ? 4 : 10);
        doc.add(title);

        for (Map.Entry<String, String> entry : meta.filters().entrySet()) {
            doc.add(new Paragraph(entry.getKey() + ": " + entry.getValue(), metaFont));
        }

        if (meta.sortLabel() != null) {
            Paragraph sortLine = new Paragraph("Sorted by: " + meta.sortLabel(), metaFont);
            sortLine.setSpacingAfter(10);
            doc.add(sortLine);
        } else if (hasMeta) {
            Paragraph spacer = new Paragraph(" ", metaFont);
            spacer.setSpacingAfter(6);
            doc.add(spacer);
        }
    }

    public static PdfPTable createHeaderTable(List<String> headers, float[] widths, Font headerFont) {
        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100);
        table.setWidths(widths);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(4);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
        return table;
    }

    public static void addCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (bg != null) cell.setBackgroundColor(bg);
        cell.setPadding(3);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }
}
