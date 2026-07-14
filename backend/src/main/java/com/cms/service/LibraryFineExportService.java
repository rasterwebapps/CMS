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

import com.cms.dto.LibraryFineDetailResponse;
import com.cms.model.enums.FineStatus;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class LibraryFineExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final List<String> HEADERS = List.of(
        "#", "Acc. No.", "Item", "Member", "Overdue Days", "Fine/Day", "Amount", "Status", "Resolved By");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<LibraryFineDetailResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Fine Register");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                LibraryFineDetailResponse f = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                ExcelExportUtil.setCell(row, 0, String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1, nvl(f.accessionNumber()), style);
                ExcelExportUtil.setCell(row, 2, nvl(f.itemTitle()), style);
                ExcelExportUtil.setCell(row, 3, nvl(f.memberName()), style);
                ExcelExportUtil.setCell(row, 4, String.valueOf(f.overdueDays()), style);
                ExcelExportUtil.setCell(row, 5, "₹" + f.finePerDay(), style);
                ExcelExportUtil.setCell(row, 6, "₹" + f.totalFine(), style);
                ExcelExportUtil.setCell(row, 7, f.status() != null ? f.status().name() : "—", style);
                ExcelExportUtil.setCell(row, 8, resolvedBy(f), style);
            }

            int[] widths = { 6, 14, 28, 22, 12, 10, 10, 12, 20 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<LibraryFineDetailResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new java.awt.Color(255, 255, 255));
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 4, 10, 20, 16, 9, 9, 9, 10, 13 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                LibraryFineDetailResponse f = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(f.accessionNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(f.itemTitle()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(f.memberName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, String.valueOf(f.overdueDays()), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, "Rs." + f.finePerDay(), dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, "Rs." + f.totalFine(), dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, f.status() != null ? f.status().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, resolvedBy(f), dataFont, rowBg, Element.ALIGN_LEFT);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Mirrors library-fines.component.ts's resolvedBy() — Waived by X / Collected <date> / — */
    private static String resolvedBy(LibraryFineDetailResponse f) {
        if (f.status() == FineStatus.WAIVED) {
            return f.waivedBy() != null ? "Waived by " + f.waivedBy() : "Waived";
        }
        if (f.status() == FineStatus.COLLECTED) {
            return f.collectedAt() != null ? "Collected " + f.collectedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate().format(DATE_FMT) : "Collected";
        }
        return "—";
    }

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
