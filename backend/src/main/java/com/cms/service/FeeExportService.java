package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

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

import com.cms.dto.FeeExplorerResponse;
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
public class FeeExportService {

    private static final NumberFormat INR = NumberFormat.getNumberInstance(new Locale("en", "IN"));

    static {
        INR.setMinimumFractionDigits(2);
        INR.setMaximumFractionDigits(2);
    }

    private static final String[] HEADERS = {
        "#", "Roll No.", "Student Name", "Program", "Sem", "Batch (Year)",
        "Total Fee (₹)", "Paid (₹)", "Pending (₹)", "Penalty (₹)", "Status",
    };

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<FeeExplorerResponse.StudentFeeSummary> rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Fee Explorer");

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
            titleCell.setCellValue("Student Fee Explorer Export");
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
            for (FeeExplorerResponse.StudentFeeSummary r : rows) {
                XSSFRow row = sheet.createRow(rowIdx);
                XSSFCellStyle style = (rowIdx % 2 == 0) ? dataStyle : altStyle;

                setCell(row, 0,  String.valueOf(rowIdx - 1), style);
                setCell(row, 1,  nvl(r.rollNumber()), style);
                setCell(row, 2,  nvl(r.studentName()), style);
                setCell(row, 3,  nvl(r.programName()), style);
                setCell(row, 4,  r.yearOfStudy() != null ? String.valueOf(r.yearOfStudy()) : "—", style);
                setCell(row, 5,  nvl(r.academicYearName()), style);
                setCell(row, 6,  fmtInr(r.totalFee()), style);
                setCell(row, 7,  fmtInr(r.totalPaid()), style);
                setCell(row, 8,  fmtInr(r.totalPending()), style);
                setCell(row, 9,  fmtInr(r.totalPenalty()), style);
                setCell(row, 10, nvl(r.allocationStatus()), style);

                rowIdx++;
            }

            int[] widths = { 6, 14, 26, 20, 6, 14, 16, 14, 14, 14, 14 };
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 2);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<FeeExplorerResponse.StudentFeeSummary> rows) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7,
                new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            Paragraph title = new Paragraph("Student Fee Explorer Export", titleFont);
            title.setSpacingAfter(10);
            doc.add(title);

            PdfPTable table = new PdfPTable(HEADERS.length);
            table.setWidthPercentage(100);
            float[] colWidths = { 3, 8, 15, 12, 4, 9, 10, 9, 9, 9, 8 };
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
            for (FeeExplorerResponse.StudentFeeSummary r : rows) {
                java.awt.Color rowBg = (idx % 2 == 0) ? altBg : null;
                addCell(table, String.valueOf(idx), dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, nvl(r.rollNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(r.studentName()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(r.programName()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, r.yearOfStudy() != null ? String.valueOf(r.yearOfStudy()) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, nvl(r.academicYearName()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, fmtInr(r.totalFee()), dataFont, rowBg, Element.ALIGN_RIGHT);
                addCell(table, fmtInr(r.totalPaid()), dataFont, rowBg, Element.ALIGN_RIGHT);
                addCell(table, fmtInr(r.totalPending()), dataFont, rowBg, Element.ALIGN_RIGHT);
                addCell(table, fmtInr(r.totalPenalty()), dataFont, rowBg, Element.ALIGN_RIGHT);
                addCell(table, nvl(r.allocationStatus()), dataFont, rowBg, Element.ALIGN_LEFT);
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

    private static String fmtInr(BigDecimal val) {
        if (val == null) return "0.00";
        return INR.format(val);
    }

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
