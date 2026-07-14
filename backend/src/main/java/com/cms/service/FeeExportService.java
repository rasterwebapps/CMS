package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.cms.dto.FeeExplorerResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class FeeExportService {

    private static final NumberFormat INR = NumberFormat.getNumberInstance(new Locale("en", "IN"));

    static {
        INR.setMinimumFractionDigits(2);
        INR.setMaximumFractionDigits(2);
    }

    private static final List<String> HEADERS = List.of(
        "#", "Roll No.", "Student Name", "Program", "Sem", "Batch (Year)",
        "Total Fee (₹)", "Paid (₹)", "Pending (₹)", "Penalty (₹)", "Status");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<FeeExplorerResponse.StudentFeeSummary> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Fee Explorer");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                FeeExplorerResponse.StudentFeeSummary r = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                ExcelExportUtil.setCell(row, 0,  String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1,  nvl(r.rollNumber()), style);
                ExcelExportUtil.setCell(row, 2,  nvl(r.studentName()), style);
                ExcelExportUtil.setCell(row, 3,  nvl(r.programName()), style);
                ExcelExportUtil.setCell(row, 4,  r.yearOfStudy() != null ? String.valueOf(r.yearOfStudy()) : "—", style);
                ExcelExportUtil.setCell(row, 5,  nvl(r.academicYearName()), style);
                ExcelExportUtil.setCell(row, 6,  fmtInr(r.totalFee()), style);
                ExcelExportUtil.setCell(row, 7,  fmtInr(r.totalPaid()), style);
                ExcelExportUtil.setCell(row, 8,  fmtInr(r.totalPending()), style);
                ExcelExportUtil.setCell(row, 9,  fmtInr(r.totalPenalty()), style);
                ExcelExportUtil.setCell(row, 10, nvl(r.allocationStatus()), style);
            }

            int[] widths = { 6, 14, 26, 20, 6, 14, 16, 14, 14, 14, 14 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<FeeExplorerResponse.StudentFeeSummary> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 3, 8, 15, 12, 4, 9, 10, 9, 9, 9, 8 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                FeeExplorerResponse.StudentFeeSummary r = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(r.rollNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.studentName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.programName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, r.yearOfStudy() != null ? String.valueOf(r.yearOfStudy()) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(r.academicYearName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, fmtInr(r.totalFee()), dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, fmtInr(r.totalPaid()), dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, fmtInr(r.totalPending()), dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, fmtInr(r.totalPenalty()), dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, nvl(r.allocationStatus()), dataFont, rowBg, Element.ALIGN_LEFT);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String fmtInr(BigDecimal val) {
        if (val == null) return "0.00";
        return INR.format(val);
    }

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
