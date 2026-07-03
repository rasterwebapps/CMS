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

import com.cms.dto.UnifiedReceiptResponse;
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
public class ReceiptExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final String[] HEADERS = {
        "#", "Date", "Receipt No.", "Payer", "Type", "Roll / Adm No.",
        "Program", "Academic Year", "Amount (₹)", "Mode", "Txn Ref", "Towards",
        "Collected By", "Category", "Refund Status",
    };

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<UnifiedReceiptResponse> rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Receipts");

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
            titleCell.setCellValue("Fee Receipts Export");
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
            for (UnifiedReceiptResponse r : rows) {
                XSSFRow row = sheet.createRow(rowIdx);
                XSSFCellStyle style = (rowIdx % 2 == 0) ? dataStyle : altStyle;

                String rollAdm = r.admissionNumber() != null ? r.admissionNumber()
                    : (r.payerIdentifier() != null ? r.payerIdentifier() : "—");

                setCell(row, 0,  String.valueOf(rowIdx - 1), style);
                setCell(row, 1,  r.paymentDate() != null ? r.paymentDate().format(DATE_FMT) : "—", style);
                setCell(row, 2,  nvl(r.receiptNumber()), style);
                setCell(row, 3,  nvl(r.payerName()), style);
                setCell(row, 4,  nvl(r.payerType()), style);
                setCell(row, 5,  rollAdm, style);
                setCell(row, 6,  nvl(r.programName()), style);
                setCell(row, 7,  nvl(r.academicYearName()), style);
                setCell(row, 8,  r.amountPaid() != null ? r.amountPaid().toPlainString() : "—", style);
                setCell(row, 9,  nvl(r.paymentMode()), style);
                setCell(row, 10, nvl(r.transactionReference()), style);
                setCell(row, 11, nvl(r.installmentsCovered()), style);
                setCell(row, 12, nvl(r.collectedBy()), style);
                setCell(row, 13, nvl(r.feeCategory()), style);
                setCell(row, 14, r.refundStatus() != null ? r.refundStatus() : "—", style);

                rowIdx++;
            }

            int[] widths = { 5, 12, 16, 22, 10, 14, 18, 16, 12, 10, 16, 16, 14, 12, 12 };
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 2);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<UnifiedReceiptResponse> rows) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 20, 20, 40, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6,
                new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 6);

            Paragraph title = new Paragraph("Fee Receipts Export", titleFont);
            title.setSpacingAfter(10);
            doc.add(title);

            PdfPTable table = new PdfPTable(HEADERS.length);
            table.setWidthPercentage(100);
            float[] colWidths = { 3, 7, 9, 12, 5, 8, 10, 9, 7, 6, 9, 9, 8, 7, 7 };
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
            for (UnifiedReceiptResponse r : rows) {
                java.awt.Color rowBg = (idx % 2 == 0) ? altBg : null;
                String rollAdm = r.admissionNumber() != null ? r.admissionNumber()
                    : (r.payerIdentifier() != null ? r.payerIdentifier() : "—");

                addCell(table, String.valueOf(idx), dataFont, rowBg, Element.ALIGN_CENTER);
                addCell(table, r.paymentDate() != null ? r.paymentDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(r.receiptNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(r.payerName()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(r.payerType()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, rollAdm, dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(r.programName()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(r.academicYearName()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, r.amountPaid() != null ? r.amountPaid().toPlainString() : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                addCell(table, nvl(r.paymentMode()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(r.transactionReference()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(r.installmentsCovered()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(r.collectedBy()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, nvl(r.feeCategory()), dataFont, rowBg, Element.ALIGN_LEFT);
                addCell(table, r.refundStatus() != null ? r.refundStatus() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
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
