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

import com.cms.dto.UnifiedReceiptResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class ReceiptExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final List<String> HEADERS = List.of(
        "#", "Date", "Receipt No.", "Payer", "Type", "Roll / Adm No.",
        "Program", "Academic Year", "Amount (₹)", "Mode", "Txn Ref", "Towards",
        "Collected By", "Category", "Refund Status");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<UnifiedReceiptResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Receipts");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                UnifiedReceiptResponse r = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                String rollAdm = r.admissionNumber() != null ? r.admissionNumber()
                    : (r.payerIdentifier() != null ? r.payerIdentifier() : "—");

                ExcelExportUtil.setCell(row, 0,  String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1,  r.paymentDate() != null ? r.paymentDate().format(DATE_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 2,  nvl(r.receiptNumber()), style);
                ExcelExportUtil.setCell(row, 3,  nvl(r.payerName()), style);
                ExcelExportUtil.setCell(row, 4,  nvl(r.payerType()), style);
                ExcelExportUtil.setCell(row, 5,  rollAdm, style);
                ExcelExportUtil.setCell(row, 6,  nvl(r.programName()), style);
                ExcelExportUtil.setCell(row, 7,  nvl(r.academicYearName()), style);
                ExcelExportUtil.setCell(row, 8,  r.amountPaid() != null ? r.amountPaid().toPlainString() : "—", style);
                ExcelExportUtil.setCell(row, 9,  nvl(r.paymentMode()), style);
                ExcelExportUtil.setCell(row, 10, nvl(r.transactionReference()), style);
                ExcelExportUtil.setCell(row, 11, nvl(r.installmentsCovered()), style);
                ExcelExportUtil.setCell(row, 12, nvl(r.collectedBy()), style);
                ExcelExportUtil.setCell(row, 13, nvl(r.feeCategory()), style);
                ExcelExportUtil.setCell(row, 14, r.refundStatus() != null ? r.refundStatus() : "—", style);
            }

            int[] widths = { 5, 12, 16, 22, 10, 14, 18, 16, 12, 10, 16, 16, 14, 12, 12 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<UnifiedReceiptResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 20, 20, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6, new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 6);

            float[] colWidths = { 3, 7, 9, 12, 5, 8, 10, 9, 7, 6, 9, 9, 8, 7, 7 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                UnifiedReceiptResponse r = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                String rollAdm = r.admissionNumber() != null ? r.admissionNumber()
                    : (r.payerIdentifier() != null ? r.payerIdentifier() : "—");

                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, r.paymentDate() != null ? r.paymentDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.receiptNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.payerName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.payerType()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, rollAdm, dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.programName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.academicYearName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, r.amountPaid() != null ? r.amountPaid().toPlainString() : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, nvl(r.paymentMode()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.transactionReference()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.installmentsCovered()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.collectedBy()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.feeCategory()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, r.refundStatus() != null ? r.refundStatus() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
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
