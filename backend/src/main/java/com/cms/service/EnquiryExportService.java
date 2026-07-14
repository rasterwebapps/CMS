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

import com.cms.dto.EnquiryResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class EnquiryExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final List<String> HEADERS = List.of(
        "#", "Name", "Phone", "Email", "Program", "Course",
        "Student Type", "Enquiry Date", "Academic Year",
        "Referral Type", "Agent", "Status", "Admission Quota");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<EnquiryResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Enquiries");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                EnquiryResponse e = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                ExcelExportUtil.setCell(row, 0, String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1, e.name(), style);
                ExcelExportUtil.setCell(row, 2, nvl(e.phone()), style);
                ExcelExportUtil.setCell(row, 3, nvl(e.email()), style);
                ExcelExportUtil.setCell(row, 4, nvl(e.programName()), style);
                ExcelExportUtil.setCell(row, 5, nvl(e.courseName()), style);
                ExcelExportUtil.setCell(row, 6, e.studentType() != null ? e.studentType().name() : "—", style);
                ExcelExportUtil.setCell(row, 7, e.enquiryDate() != null ? e.enquiryDate().format(DATE_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 8, nvl(e.academicYearName()), style);
                ExcelExportUtil.setCell(row, 9, nvl(e.referralTypeName()), style);
                ExcelExportUtil.setCell(row, 10, nvl(e.agentName()), style);
                ExcelExportUtil.setCell(row, 11, e.status() != null ? e.status().name() : "—", style);
                ExcelExportUtil.setCell(row, 12, e.admissionQuota() != null ? e.admissionQuota().name() : "—", style);
            }

            int[] widths = { 8, 28, 16, 30, 20, 22, 14, 14, 16, 18, 18, 18, 16 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<EnquiryResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new java.awt.Color(255, 255, 255));
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 4, 14, 9, 14, 11, 11, 8, 8, 9, 10, 10, 10, 8 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                EnquiryResponse e = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, e.name(), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(e.phone()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(e.email()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(e.programName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(e.courseName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, e.studentType() != null ? e.studentType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, e.enquiryDate() != null ? e.enquiryDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(e.academicYearName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(e.referralTypeName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(e.agentName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, e.status() != null ? e.status().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, e.admissionQuota() != null ? e.admissionQuota().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
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
