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

import com.cms.dto.LibraryIssueResponse;
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
public class LibraryIssueExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final String[] HEADERS = {
        "#", "Acc. No.", "Item", "Type", "Member", "Member Type",
        "Issued", "Due", "Returned", "Status", "Fine",
    };

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<LibraryIssueResponse> rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Issue Register");

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
            titleCell.setCellValue("Issue Register Export");
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
            for (LibraryIssueResponse issue : rows) {
                XSSFRow row = sheet.createRow(rowIdx);
                XSSFCellStyle style = (rowIdx % 2 == 0) ? dataStyle : altStyle;

                setCell(row, 0, String.valueOf(rowIdx - 1), style);
                setCell(row, 1, nvl(issue.accessionNumber()), style);
                setCell(row, 2, nvl(issue.itemTitle()), style);
                setCell(row, 3, issue.itemType() != null ? issue.itemType().name() : "—", style);
                setCell(row, 4, memberName(issue), style);
                setCell(row, 5, issue.memberType() != null ? issue.memberType().name() : "—", style);
                setCell(row, 6, issue.issuedDate() != null ? issue.issuedDate().format(DATE_FMT) : "—", style);
                setCell(row, 7, issue.dueDate() != null ? issue.dueDate().format(DATE_FMT) : "—", style);
                setCell(row, 8, issue.returnedDate() != null ? issue.returnedDate().format(DATE_FMT) : "—", style);
                setCell(row, 9, issue.status() != null ? issue.status().name() : "—", style);
                setCell(row, 10, issue.fine() != null ? "₹" + issue.fine().totalFine() : "—", style);

                rowIdx++;
            }

            int[] widths = { 6, 14, 28, 10, 22, 12, 12, 12, 12, 12, 12 };
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 2);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<LibraryIssueResponse> rows) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8,
                new java.awt.Color(255, 255, 255));
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            Paragraph title = new Paragraph("Issue Register Export", titleFont);
            title.setSpacingAfter(10);
            doc.add(title);

            PdfPTable table = new PdfPTable(HEADERS.length);
            table.setWidthPercentage(100);
            float[] colWidths = { 4, 10, 18, 8, 15, 10, 9, 9, 9, 9, 8 };
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
            for (LibraryIssueResponse issue : rows) {
                java.awt.Color rowBg = (idx % 2 == 0) ? altBg : null;
                addCell(table, String.valueOf(idx), dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, nvl(issue.accessionNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(issue.itemTitle()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, issue.itemType() != null ? issue.itemType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, memberName(issue), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, issue.memberType() != null ? issue.memberType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, issue.issuedDate() != null ? issue.issuedDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, issue.dueDate() != null ? issue.dueDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, issue.returnedDate() != null ? issue.returnedDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, issue.status() != null ? issue.status().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, issue.fine() != null ? "Rs." + issue.fine().totalFine() : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                idx++;
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
