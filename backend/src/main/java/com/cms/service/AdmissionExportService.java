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

import com.cms.dto.AdmissionResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class AdmissionExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final List<String> HEADERS = List.of(
        "#", "Student Name", "Admission No.", "Roll No.", "Program", "Course",
        "Sem", "Student Type", "Joining Year", "Expected Completion",
        "Application Date", "Status");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<AdmissionResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Admissions");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                AdmissionResponse a = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                ExcelExportUtil.setCell(row, 0,  String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1,  nvl(a.studentName()), style);
                ExcelExportUtil.setCell(row, 2,  nvl(a.admissionNumber()), style);
                ExcelExportUtil.setCell(row, 3,  nvl(a.rollNumber()), style);
                ExcelExportUtil.setCell(row, 4,  nvl(a.programName()), style);
                ExcelExportUtil.setCell(row, 5,  nvl(a.courseName()), style);
                ExcelExportUtil.setCell(row, 6,  a.semester() != null ? String.valueOf(a.semester()) : "—", style);
                ExcelExportUtil.setCell(row, 7,  nvl(a.studentType()), style);
                ExcelExportUtil.setCell(row, 8,  nvl(a.joiningAcademicYearName()), style);
                ExcelExportUtil.setCell(row, 9,  a.expectedCompletionYear() != null ? String.valueOf(a.expectedCompletionYear()) : "—", style);
                ExcelExportUtil.setCell(row, 10, a.applicationDate() != null ? a.applicationDate().format(DATE_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 11, nvl(a.studentStatus()), style);
            }

            int[] widths = { 6, 26, 16, 14, 20, 22, 6, 14, 16, 18, 16, 14 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<AdmissionResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 3, 14, 9, 8, 11, 12, 4, 8, 9, 10, 9, 8 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                AdmissionResponse a = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(a.studentName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(a.admissionNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(a.rollNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(a.programName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(a.courseName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, a.semester() != null ? String.valueOf(a.semester()) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(a.studentType()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(a.joiningAcademicYearName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, a.expectedCompletionYear() != null ? String.valueOf(a.expectedCompletionYear()) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, a.applicationDate() != null ? a.applicationDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(a.studentStatus()), dataFont, rowBg, Element.ALIGN_LEFT);
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
