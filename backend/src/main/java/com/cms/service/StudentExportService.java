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

import com.cms.dto.StudentResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class StudentExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final List<String> HEADERS = List.of(
        "#", "Name", "Admission No.", "Roll No.", "Program", "Course",
        "Sem", "Admission Date", "Phone", "Email", "Status", "Batch (Year)");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<StudentResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Students");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                StudentResponse s = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                ExcelExportUtil.setCell(row, 0,  String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1,  nvl(s.fullName()), style);
                ExcelExportUtil.setCell(row, 2,  nvl(s.admissionNumber()), style);
                ExcelExportUtil.setCell(row, 3,  nvl(s.rollNumber()), style);
                ExcelExportUtil.setCell(row, 4,  nvl(s.programName()), style);
                ExcelExportUtil.setCell(row, 5,  nvl(s.courseName()), style);
                ExcelExportUtil.setCell(row, 6,  s.yearOfStudy() != null ? String.valueOf(s.yearOfStudy()) : "—", style);
                ExcelExportUtil.setCell(row, 7,  s.admissionDate() != null ? s.admissionDate().format(DATE_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 8,  nvl(s.phone()), style);
                ExcelExportUtil.setCell(row, 9,  nvl(s.email()), style);
                ExcelExportUtil.setCell(row, 10, s.status() != null ? s.status().name() : "—", style);
                ExcelExportUtil.setCell(row, 11, nvl(s.admissionAcademicYearName()), style);
            }

            int[] widths = { 6, 26, 16, 14, 20, 20, 6, 16, 14, 24, 12, 14 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<StudentResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 3, 14, 9, 8, 11, 11, 4, 9, 8, 12, 7, 8 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                StudentResponse s = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(s.fullName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(s.admissionNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(s.rollNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(s.programName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(s.courseName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, s.yearOfStudy() != null ? String.valueOf(s.yearOfStudy()) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, s.admissionDate() != null ? s.admissionDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(s.phone()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(s.email()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, s.status() != null ? s.status().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(s.admissionAcademicYearName()), dataFont, rowBg, Element.ALIGN_LEFT);
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
}
