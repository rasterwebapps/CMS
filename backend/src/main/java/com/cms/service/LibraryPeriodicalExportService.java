package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.cms.dto.LibraryPeriodicalResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class LibraryPeriodicalExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final List<String> HEADERS = List.of(
        "#", "Acc. No.", "Journal Name", "Type", "Volume / Issue", "Year", "Status", "Received");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<LibraryPeriodicalResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Journals & Periodicals");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                LibraryPeriodicalResponse p = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                ExcelExportUtil.setCell(row, 0, String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1, nvl(p.accessionNumber()), style);
                ExcelExportUtil.setCell(row, 2, nvl(p.journalName()), style);
                ExcelExportUtil.setCell(row, 3, p.journalType() != null ? p.journalType().name() : "—", style);
                ExcelExportUtil.setCell(row, 4, volumeIssue(p), style);
                ExcelExportUtil.setCell(row, 5, p.year() != null ? String.valueOf(p.year()) : "—", style);
                ExcelExportUtil.setCell(row, 6, p.subscriptionStatus() != null ? p.subscriptionStatus().name() : "—", style);
                ExcelExportUtil.setCell(row, 7, p.receivedDate() != null ? p.receivedDate().format(DATE_FMT) : "—", style);
            }

            int[] widths = { 6, 16, 32, 14, 20, 10, 14, 14 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<LibraryPeriodicalResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new java.awt.Color(255, 255, 255));
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 4, 12, 24, 12, 16, 7, 11, 12 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                LibraryPeriodicalResponse p = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(p.accessionNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(p.journalName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, p.journalType() != null ? p.journalType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, volumeIssue(p), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, p.year() != null ? String.valueOf(p.year()) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, p.subscriptionStatus() != null ? p.subscriptionStatus().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, p.receivedDate() != null ? p.receivedDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String volumeIssue(LibraryPeriodicalResponse p) {
        List<String> parts = new ArrayList<>();
        if (p.volumeNumber() != null && !p.volumeNumber().isBlank()) parts.add("Vol. " + p.volumeNumber());
        if (p.issueNumber() != null && !p.issueNumber().isBlank()) parts.add("No. " + p.issueNumber());
        if (p.monthRange() != null && !p.monthRange().isBlank()) parts.add("(" + p.monthRange() + ")");
        return parts.isEmpty() ? "—" : String.join(" ", parts);
    }

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
