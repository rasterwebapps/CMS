package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.cms.dto.ScholarshipTypeResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class ScholarshipTypeExportService {

    private static final List<String> HEADERS = List.of(
        "#", "Code", "Name", "Govt Scheme", "Scheme Code",
        "Discount Type", "Discount Value", "Max Amt/Year (₹)",
        "Renewal Req.", "Active", "Application Mode");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<ScholarshipTypeResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Scholarship Types");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                ScholarshipTypeResponse s = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();
                ExcelExportUtil.setCell(row, 0,  String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1,  nvl(s.code()), style);
                ExcelExportUtil.setCell(row, 2,  nvl(s.name()), style);
                ExcelExportUtil.setCell(row, 3,  Boolean.TRUE.equals(s.govtScheme()) ? "Yes" : "No", style);
                ExcelExportUtil.setCell(row, 4,  nvl(s.schemeCode()), style);
                ExcelExportUtil.setCell(row, 5,  s.discountType() != null ? s.discountType().name() : "—", style);
                ExcelExportUtil.setCell(row, 6,  s.discountValue() != null ? s.discountValue().toPlainString() : "—", style);
                ExcelExportUtil.setCell(row, 7,  s.maxAmountPerYear() != null ? s.maxAmountPerYear().toPlainString() : "—", style);
                ExcelExportUtil.setCell(row, 8,  Boolean.TRUE.equals(s.renewalRequired()) ? "Yes" : "No", style);
                ExcelExportUtil.setCell(row, 9,  Boolean.TRUE.equals(s.active()) ? "Yes" : "No", style);
                ExcelExportUtil.setCell(row, 10, s.applicationMode() != null ? s.applicationMode().name() : "—", style);
            }

            int[] widths = { 5, 14, 26, 12, 14, 14, 14, 16, 12, 8, 16 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<ScholarshipTypeResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 25, 25, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 3, 8, 15, 7, 8, 8, 8, 9, 7, 5, 9 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                ScholarshipTypeResponse s = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(s.code()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(s.name()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, Boolean.TRUE.equals(s.govtScheme()) ? "Yes" : "No", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(s.schemeCode()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, s.discountType() != null ? s.discountType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, s.discountValue() != null ? s.discountValue().toPlainString() : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, s.maxAmountPerYear() != null ? s.maxAmountPerYear().toPlainString() : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, Boolean.TRUE.equals(s.renewalRequired()) ? "Yes" : "No", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, Boolean.TRUE.equals(s.active()) ? "Yes" : "No", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, s.applicationMode() != null ? s.applicationMode().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
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
