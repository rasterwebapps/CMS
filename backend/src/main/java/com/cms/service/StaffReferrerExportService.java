package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.cms.dto.StaffReferrerResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class StaffReferrerExportService {

    private static final List<String> HEADERS = List.of(
        "#", "Name", "Phone", "Email", "Emp. Code",
        "Institution", "Commission (₹)", "Active", "PAN No.", "Bank Name");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<StaffReferrerResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Staff Referrers");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                StaffReferrerResponse r = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();
                ExcelExportUtil.setCell(row, 0, String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1, nvl(r.name()), style);
                ExcelExportUtil.setCell(row, 2, nvl(r.phone()), style);
                ExcelExportUtil.setCell(row, 3, nvl(r.email()), style);
                ExcelExportUtil.setCell(row, 4, nvl(r.employeeCode()), style);
                ExcelExportUtil.setCell(row, 5, nvl(r.institutionName()), style);
                ExcelExportUtil.setCell(row, 6, r.commissionAmount() != null ? r.commissionAmount().toPlainString() : "—", style);
                ExcelExportUtil.setCell(row, 7, Boolean.TRUE.equals(r.isActive()) ? "Yes" : "No", style);
                ExcelExportUtil.setCell(row, 8, nvl(r.panNumber()), style);
                ExcelExportUtil.setCell(row, 9, nvl(r.bankName()), style);
            }

            int[] widths = { 5, 24, 14, 24, 14, 22, 16, 8, 14, 18 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<StaffReferrerResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 25, 25, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 3, 14, 8, 14, 8, 13, 9, 5, 8, 11 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                StaffReferrerResponse r = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(r.name()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.phone()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.email()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.employeeCode()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.institutionName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, r.commissionAmount() != null ? r.commissionAmount().toPlainString() : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, Boolean.TRUE.equals(r.isActive()) ? "Yes" : "No", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(r.panNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.bankName()), dataFont, rowBg, Element.ALIGN_LEFT);
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
