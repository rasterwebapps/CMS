package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.cms.dto.LibraryIssueResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class LibraryIssueExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final List<String> HEADERS = List.of(
        "#", "Acc. No.", "Item", "Type", "Member", "Member Type",
        "Issued", "Due", "Returned", "Status", "Fine");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<LibraryIssueResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Issue Register");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                LibraryIssueResponse issue = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                ExcelExportUtil.setCell(row, 0, String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1, nvl(issue.accessionNumber()), style);
                ExcelExportUtil.setCell(row, 2, nvl(issue.itemTitle()), style);
                ExcelExportUtil.setCell(row, 3, issue.itemType() != null ? issue.itemType().name() : "—", style);
                ExcelExportUtil.setCell(row, 4, memberName(issue), style);
                ExcelExportUtil.setCell(row, 5, issue.memberType() != null ? issue.memberType().name() : "—", style);
                ExcelExportUtil.setCell(row, 6, issue.issuedDate() != null ? issue.issuedDate().format(DATE_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 7, issue.dueDate() != null ? issue.dueDate().format(DATE_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 8, issue.returnedDate() != null ? issue.returnedDate().format(DATE_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 9, issue.status() != null ? issue.status().name() : "—", style);
                ExcelExportUtil.setCell(row, 10, issue.fine() != null ? "₹" + issue.fine().totalFine() : "—", style);
            }

            int[] widths = { 6, 14, 28, 10, 22, 12, 12, 12, 12, 12, 12 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<LibraryIssueResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new java.awt.Color(255, 255, 255));
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 4, 10, 18, 8, 15, 10, 9, 9, 9, 9, 8 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                LibraryIssueResponse issue = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(issue.accessionNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(issue.itemTitle()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, issue.itemType() != null ? issue.itemType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, memberName(issue), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, issue.memberType() != null ? issue.memberType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, issue.issuedDate() != null ? issue.issuedDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, issue.dueDate() != null ? issue.dueDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, issue.returnedDate() != null ? issue.returnedDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, issue.status() != null ? issue.status().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, issue.fine() != null ? "Rs." + issue.fine().totalFine() : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String memberName(LibraryIssueResponse issue) {
        String name = issue.studentName() != null ? issue.studentName() : issue.facultyName();
        return nvl(name);
    }

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
