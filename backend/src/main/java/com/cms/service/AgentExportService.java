package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.cms.dto.AgentResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class AgentExportService {

    private static final List<String> HEADERS = List.of(
        "#", "Name", "Phone", "Email", "Area", "Locality",
        "Allotted Seats", "Commission (₹)", "Active", "PAN No.", "Bank Name");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<AgentResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Agents");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                AgentResponse a = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();
                ExcelExportUtil.setCell(row, 0,  String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1,  nvl(a.name()), style);
                ExcelExportUtil.setCell(row, 2,  nvl(a.phone()), style);
                ExcelExportUtil.setCell(row, 3,  nvl(a.email()), style);
                ExcelExportUtil.setCell(row, 4,  nvl(a.area()), style);
                ExcelExportUtil.setCell(row, 5,  nvl(a.locality()), style);
                ExcelExportUtil.setCell(row, 6,  a.allottedSeats() != null ? String.valueOf(a.allottedSeats()) : "—", style);
                ExcelExportUtil.setCell(row, 7,  a.commissionAmount() != null ? a.commissionAmount().toPlainString() : "—", style);
                ExcelExportUtil.setCell(row, 8,  Boolean.TRUE.equals(a.isActive()) ? "Yes" : "No", style);
                ExcelExportUtil.setCell(row, 9,  nvl(a.panNumber()), style);
                ExcelExportUtil.setCell(row, 10, nvl(a.bankName()), style);
            }

            int[] widths = { 5, 24, 14, 24, 16, 16, 14, 16, 8, 14, 18 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<AgentResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 25, 25, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 3, 14, 8, 14, 10, 10, 8, 9, 5, 8, 11 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                AgentResponse a = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(a.name()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(a.phone()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(a.email()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(a.area()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(a.locality()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, a.allottedSeats() != null ? String.valueOf(a.allottedSeats()) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, a.commissionAmount() != null ? a.commissionAmount().toPlainString() : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, Boolean.TRUE.equals(a.isActive()) ? "Yes" : "No", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(a.panNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(a.bankName()), dataFont, rowBg, Element.ALIGN_LEFT);
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
