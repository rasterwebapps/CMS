package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

import com.cms.dto.ScholarshipTypeResponse;
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
public class ScholarshipTypeExportService {

    private static final String[] HEADERS = {
        "#", "Code", "Name", "Govt Scheme", "Scheme Code",
        "Discount Type", "Discount Value", "Max Amt/Year (₹)",
        "Renewal Req.", "Active", "Application Mode",
    };

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<ScholarshipTypeResponse> rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Scholarship Types");

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

            XSSFRow titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(22);
            var titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Scholarship Types Export");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            XSSFRow headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(18);
            for (int i = 0; i < HEADERS.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 2;
            for (ScholarshipTypeResponse s : rows) {
                XSSFRow row = sheet.createRow(rowIdx);
                XSSFCellStyle style = (rowIdx % 2 == 0) ? dataStyle : altStyle;
                setCell(row, 0,  String.valueOf(rowIdx - 1), style);
                setCell(row, 1,  nvl(s.code()), style);
                setCell(row, 2,  nvl(s.name()), style);
                setCell(row, 3,  Boolean.TRUE.equals(s.govtScheme()) ? "Yes" : "No", style);
                setCell(row, 4,  nvl(s.schemeCode()), style);
                setCell(row, 5,  s.discountType() != null ? s.discountType().name() : "—", style);
                setCell(row, 6,  s.discountValue() != null ? s.discountValue().toPlainString() : "—", style);
                setCell(row, 7,  s.maxAmountPerYear() != null ? s.maxAmountPerYear().toPlainString() : "—", style);
                setCell(row, 8,  Boolean.TRUE.equals(s.renewalRequired()) ? "Yes" : "No", style);
                setCell(row, 9,  Boolean.TRUE.equals(s.active()) ? "Yes" : "No", style);
                setCell(row, 10, s.applicationMode() != null ? s.applicationMode().name() : "—", style);
                rowIdx++;
            }

            int[] widths = { 5, 14, 26, 12, 14, 14, 14, 16, 12, 8, 16 };
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 2);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<ScholarshipTypeResponse> rows) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 25, 25, 40, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7,
                new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            Paragraph title = new Paragraph("Scholarship Types Export", titleFont);
            title.setSpacingAfter(10);
            doc.add(title);

            PdfPTable table = new PdfPTable(HEADERS.length);
            table.setWidthPercentage(100);
            float[] colWidths = { 3, 8, 15, 7, 8, 8, 8, 9, 7, 5, 9 };
            table.setWidths(colWidths);

            java.awt.Color headerBg = new java.awt.Color(13, 27, 62);
            java.awt.Color altBg    = new java.awt.Color(235, 241, 255);

            for (String h : HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(3);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            int idx = 1;
            for (ScholarshipTypeResponse s : rows) {
                java.awt.Color rowBg = (idx % 2 == 0) ? altBg : null;
                addCell(table, String.valueOf(idx), dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, nvl(s.code()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(s.name()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, Boolean.TRUE.equals(s.govtScheme()) ? "Yes" : "No", dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, nvl(s.schemeCode()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, s.discountType() != null ? s.discountType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, s.discountValue() != null ? s.discountValue().toPlainString() : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                addCell(table, s.maxAmountPerYear() != null ? s.maxAmountPerYear().toPlainString() : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                addCell(table, Boolean.TRUE.equals(s.renewalRequired()) ? "Yes" : "No", dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, Boolean.TRUE.equals(s.active()) ? "Yes" : "No", dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, s.applicationMode() != null ? s.applicationMode().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
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
