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

import com.cms.dto.EquipmentResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class EquipmentExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final List<String> HEADERS = List.of(
        "#", "Name", "Asset Code", "Serial No.", "Category", "Lab",
        "Manufacturer", "Model", "Status", "Purchase Date",
        "Purchase Price (₹)", "Warranty Expiry", "Location");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<EquipmentResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Equipment");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                EquipmentResponse e = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();
                ExcelExportUtil.setCell(row, 0,  String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1,  nvl(e.name()), style);
                ExcelExportUtil.setCell(row, 2,  nvl(e.assetCode()), style);
                ExcelExportUtil.setCell(row, 3,  nvl(e.serialNumber()), style);
                ExcelExportUtil.setCell(row, 4,  e.category() != null ? e.category().name() : "—", style);
                ExcelExportUtil.setCell(row, 5,  nvl(e.labName()), style);
                ExcelExportUtil.setCell(row, 6,  nvl(e.manufacturer()), style);
                ExcelExportUtil.setCell(row, 7,  nvl(e.model()), style);
                ExcelExportUtil.setCell(row, 8,  e.status() != null ? e.status().name() : "—", style);
                ExcelExportUtil.setCell(row, 9,  e.purchaseDate() != null ? e.purchaseDate().format(DATE_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 10, e.purchasePrice() != null ? e.purchasePrice().toPlainString() : "—", style);
                ExcelExportUtil.setCell(row, 11, e.warrantyExpiry() != null ? e.warrantyExpiry().format(DATE_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 12, nvl(e.location()), style);
            }

            int[] widths = { 5, 22, 14, 16, 14, 16, 16, 16, 12, 14, 16, 14, 18 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<EquipmentResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 20, 20, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 3, 13, 8, 9, 8, 9, 9, 9, 7, 8, 9, 8, 10 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                EquipmentResponse e = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(e.name()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(e.assetCode()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(e.serialNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, e.category() != null ? e.category().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(e.labName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(e.manufacturer()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(e.model()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, e.status() != null ? e.status().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, e.purchaseDate() != null ? e.purchaseDate().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, e.purchasePrice() != null ? e.purchasePrice().toPlainString() : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, e.warrantyExpiry() != null ? e.warrantyExpiry().format(DATE_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(e.location()), dataFont, rowBg, Element.ALIGN_LEFT);
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
