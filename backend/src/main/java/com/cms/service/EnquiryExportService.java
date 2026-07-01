package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.cms.dto.EnquiryResponse;
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

@Service
public class EnquiryExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final String[] HEADERS = {
        "#", "Name", "Phone", "Email", "Program", "Course",
        "Student Type", "Enquiry Date", "Academic Year",
        "Referral Type", "Agent", "Status", "Admission Quota",
    };

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<EnquiryResponse> rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Enquiries");

            // ── Styles ─────────────────────────────────────────────────────
            XSSFCellStyle titleStyle = wb.createCellStyle();
            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            XSSFCellStyle dataStyle = wb.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.HAIR);

            XSSFCellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            altStyle.setBorderBottom(BorderStyle.THIN);
            altStyle.setBorderRight(BorderStyle.HAIR);

            // ── Title row ──────────────────────────────────────────────────
            XSSFRow titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(22);
            var titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Student Enquiries Export");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            // ── Header row ─────────────────────────────────────────────────
            XSSFRow headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(18);
            for (int i = 0; i < HEADERS.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Data rows ──────────────────────────────────────────────────
            int rowIdx = 2;
            for (EnquiryResponse e : rows) {
                XSSFRow row = sheet.createRow(rowIdx);
                XSSFCellStyle style = (rowIdx % 2 == 0) ? dataStyle : altStyle;

                setCell(row, 0, String.valueOf(rowIdx - 1), style);
                setCell(row, 1, e.name(), style);
                setCell(row, 2, nvl(e.phone()), style);
                setCell(row, 3, nvl(e.email()), style);
                setCell(row, 4, nvl(e.programName()), style);
                setCell(row, 5, nvl(e.courseName()), style);
                setCell(row, 6, e.studentType() != null ? e.studentType().name() : "—", style);
                setCell(row, 7, e.enquiryDate() != null ? e.enquiryDate().format(DATE_FMT) : "—", style);
                setCell(row, 8, nvl(e.academicYearName()), style);
                setCell(row, 9, nvl(e.referralTypeName()), style);
                setCell(row, 10, nvl(e.agentName()), style);
                setCell(row, 11, e.status() != null ? e.status().name() : "—", style);
                setCell(row, 12, e.admissionQuota() != null ? e.admissionQuota().name() : "—", style);

                rowIdx++;
            }

            // ── Column widths ──────────────────────────────────────────────
            int[] widths = { 8, 28, 16, 30, 20, 22, 14, 14, 16, 18, 18, 18, 16 };
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 2);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<EnquiryResponse> rows) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8,
                new java.awt.Color(255, 255, 255));
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            Paragraph title = new Paragraph("Student Enquiries Export", titleFont);
            title.setSpacingAfter(10);
            doc.add(title);

            PdfPTable table = new PdfPTable(HEADERS.length);
            table.setWidthPercentage(100);
            float[] colWidths = { 4, 14, 9, 14, 11, 11, 8, 8, 9, 10, 10, 10, 8 };
            table.setWidths(colWidths);

            java.awt.Color headerBg = new java.awt.Color(13, 27, 62);
            java.awt.Color altBg = new java.awt.Color(235, 241, 255);

            for (String h : HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(4);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            int idx = 1;
            for (EnquiryResponse e : rows) {
                java.awt.Color rowBg = (idx % 2 == 0) ? altBg : null;
                addCell(table, String.valueOf(idx), dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, e.name(), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(e.phone()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(e.email()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(e.programName()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(e.courseName()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, e.studentType() != null ? e.studentType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, e.enquiryDate() != null ? e.enquiryDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, nvl(e.academicYearName()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(e.referralTypeName()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(e.agentName()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, e.status() != null ? e.status().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, e.admissionQuota() != null ? e.admissionQuota().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                idx++;
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setCell(XSSFRow row, int col, String value, XSSFCellStyle style) {
        var cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void addCell(PdfPTable table, String text, Font font,
                                 java.awt.Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (bg != null) cell.setBackgroundColor(bg);
        cell.setPadding(3);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
