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

import com.cms.dto.FacultyDocumentReviewSummary;
import com.cms.dto.FacultyResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class FacultyExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final List<String> HEADERS = List.of(
        "#", "Emp. Code", "Full Name", "Phone", "Email", "Speciality",
        "Designation", "Type", "Qualification", "NRTS No.", "Status",
        "Joining Date", "Doc Review");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<FacultyResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Faculty");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                FacultyResponse f = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();
                ExcelExportUtil.setCell(row, 0,  String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1,  nvl(f.employeeCode()), style);
                ExcelExportUtil.setCell(row, 2,  nvl(f.fullName()), style);
                ExcelExportUtil.setCell(row, 3,  nvl(f.phone()), style);
                ExcelExportUtil.setCell(row, 4,  nvl(f.email()), style);
                ExcelExportUtil.setCell(row, 5,  nvl(f.specialityName()), style);
                ExcelExportUtil.setCell(row, 6,  nvl(f.designationName()), style);
                ExcelExportUtil.setCell(row, 7,  f.facultyType() != null ? f.facultyType().name() : "—", style);
                ExcelExportUtil.setCell(row, 8,  f.highestQualification() != null ? f.highestQualification().name() : "—", style);
                ExcelExportUtil.setCell(row, 9,  nvl(f.nrtsNumber()), style);
                ExcelExportUtil.setCell(row, 10, f.status() != null ? f.status().name() : "—", style);
                ExcelExportUtil.setCell(row, 11, f.joiningDate() != null ? f.joiningDate().format(DATE_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 12, docReviewLabel(f.documentReview()), style);
            }

            int[] widths = { 5, 14, 26, 14, 24, 18, 18, 12, 18, 14, 12, 14, 12 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<FacultyResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 25, 25, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 3, 8, 14, 8, 14, 10, 10, 7, 10, 8, 7, 8, 8 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                FacultyResponse f = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(f.employeeCode()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(f.fullName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(f.phone()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(f.email()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(f.specialityName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(f.designationName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, f.facultyType() != null ? f.facultyType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, f.highestQualification() != null ? f.highestQualification().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(f.nrtsNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, f.status() != null ? f.status().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, f.joiningDate() != null ? f.joiningDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, docReviewLabel(f.documentReview()), dataFont, rowBg, Element.ALIGN_LEFT);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private static String docReviewLabel(FacultyDocumentReviewSummary d) {
        if (d == null) return "—";
        if (d.allRequiredDocumentsVerified()) return "All Verified";
        if (d.hasRejectedDocuments()) return "Rejected: " + d.rejectedCount();
        if (d.hasPendingVerification()) return "Pending: " + d.pendingVerificationCount();
        if (d.missingRequiredCount() > 0) return "Missing: " + d.missingRequiredCount();
        return "—";
    }
}
