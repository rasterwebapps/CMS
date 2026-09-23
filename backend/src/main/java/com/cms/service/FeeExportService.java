package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.cms.dto.FeeExplorerResponse;
import com.cms.dto.FeeExplorerSemesterWiseRow;
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

    private static final List<String> SEMESTER_WISE_BASE_HEADERS = List.of(
        "#", "Roll No.", "Student Name", "Program", "Batch (Year)");

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

    public byte[] toExcelSemesterWise(List<FeeExplorerSemesterWiseRow> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            int maxSemesters = maxSemesterCount(rows);
            List<String> headers = semesterWiseHeaders(maxSemesters);

            XSSFSheet sheet = wb.createSheet("Fee Explorer - Sem-wise");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, headers.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, headers);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                FeeExplorerSemesterWiseRow r = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                ExcelExportUtil.setCell(row, 0, String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1, nvl(r.rollNumber()), style);
                ExcelExportUtil.setCell(row, 2, nvl(r.studentName()), style);
                ExcelExportUtil.setCell(row, 3, nvl(r.programName()), style);
                ExcelExportUtil.setCell(row, 4, nvl(r.academicYearName()), style);

                int col = 5;
                for (int s = 0; s < maxSemesters; s++) {
                    boolean hasSemester = s < r.semesters().size();
                    var sem = hasSemester ? r.semesters().get(s) : null;
                    ExcelExportUtil.setCell(row, col++, hasSemester ? fmtInr(sem.fee())     : "—", style);
                    ExcelExportUtil.setCell(row, col++, hasSemester ? fmtInr(sem.paid())    : "—", style);
                    ExcelExportUtil.setCell(row, col++, hasSemester ? fmtInr(sem.pending()) : "—", style);
                }
            }

            int[] widths = new int[5 + maxSemesters * 3];
            int[] baseWidths = { 6, 14, 26, 20, 14 };
            System.arraycopy(baseWidths, 0, widths, 0, baseWidths.length);
            for (int s = 0; s < maxSemesters; s++) {
                int col = 5 + s * 3;
                widths[col] = 14; widths[col + 1] = 14; widths[col + 2] = 14;
            }
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

    public byte[] toPdfSemesterWise(List<FeeExplorerSemesterWiseRow> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            int maxSemesters = maxSemesterCount(rows);
            List<String> headers = semesterWiseHeaders(maxSemesters);

            float[] colWidths = new float[5 + maxSemesters * 3];
            float[] baseWidths = { 3, 8, 15, 12, 9 };
            System.arraycopy(baseWidths, 0, colWidths, 0, baseWidths.length);
            for (int s = 0; s < maxSemesters; s++) {
                int col = 5 + s * 3;
                colWidths[col] = 8; colWidths[col + 1] = 8; colWidths[col + 2] = 8;
            }
            PdfPTable table = PdfExportUtil.createHeaderTable(headers, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                FeeExplorerSemesterWiseRow r = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(r.rollNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.studentName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.programName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.academicYearName()), dataFont, rowBg, Element.ALIGN_LEFT);

                for (int s = 0; s < maxSemesters; s++) {
                    boolean hasSemester = s < r.semesters().size();
                    var sem = hasSemester ? r.semesters().get(s) : null;
                    PdfExportUtil.addCell(table, hasSemester ? fmtInr(sem.fee())     : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                    PdfExportUtil.addCell(table, hasSemester ? fmtInr(sem.paid())    : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                    PdfExportUtil.addCell(table, hasSemester ? fmtInr(sem.pending()) : "—", dataFont, rowBg, Element.ALIGN_RIGHT);
                }
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int maxSemesterCount(List<FeeExplorerSemesterWiseRow> rows) {
        return rows.stream().mapToInt(r -> r.semesters().size()).max().orElse(0);
    }

    /** Base student columns, then a Fee/Paid/Pending column block per semester position (Sem 1,
     *  Sem 2, ...) up to the widest program in this export — generic positional labels since
     *  different programs' SemesterFee.semesterLabel text isn't guaranteed to line up column-for-
     *  column at the same position. */
    private static List<String> semesterWiseHeaders(int maxSemesters) {
        List<String> headers = new ArrayList<>(SEMESTER_WISE_BASE_HEADERS);
        for (int s = 1; s <= maxSemesters; s++) {
            headers.add("Sem " + s + " Fee (₹)");
            headers.add("Sem " + s + " Paid (₹)");
            headers.add("Sem " + s + " Pending (₹)");
        }
        return headers;
    }

    private static String fmtInr(BigDecimal val) {
        if (val == null) return "0.00";
        return INR.format(val);
    }

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
