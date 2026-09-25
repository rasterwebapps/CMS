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

import com.cms.dto.ClassScheduleOccurrenceResponse;
import com.cms.dto.ClassScheduleResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class ClassScheduleExportService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final List<String> HEADERS = List.of(
        "#", "Day", "Type", "Room", "Subject", "Code", "Faculty", "Batch", "Start", "End", "Term");

    private static final List<String> OCCURRENCE_HEADERS = List.of(
        "#", "Date", "Type", "Room", "Subject", "Code", "Faculty", "Batch", "Start", "End", "Status");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<ClassScheduleResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Class Schedules");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                ClassScheduleResponse r = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                ExcelExportUtil.setCell(row, 0, String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1, r.dayOfWeek() != null ? r.dayOfWeek().name() : "—", style);
                ExcelExportUtil.setCell(row, 2, r.sessionType() != null ? r.sessionType().name() : "—", style);
                ExcelExportUtil.setCell(row, 3, nvl(r.roomName()), style);
                ExcelExportUtil.setCell(row, 4, nvl(r.subjectName()), style);
                ExcelExportUtil.setCell(row, 5, nvl(r.subjectCode()), style);
                ExcelExportUtil.setCell(row, 6, nvl(r.facultyName()), style);
                ExcelExportUtil.setCell(row, 7, nvl(r.batchName()), style);
                ExcelExportUtil.setCell(row, 8, r.startTime() != null ? r.startTime().format(TIME_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 9, r.endTime() != null ? r.endTime().format(TIME_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 10, nvl(r.termInstanceLabel()), style);
            }

            int[] widths = { 6, 12, 12, 20, 24, 12, 22, 16, 12, 12, 20 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<ClassScheduleResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new java.awt.Color(255, 255, 255));
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 3, 8, 8, 14, 16, 8, 15, 11, 8, 8, 13 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                ClassScheduleResponse r = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, r.dayOfWeek() != null ? r.dayOfWeek().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, r.sessionType() != null ? r.sessionType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.roomName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.subjectName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.subjectCode()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.facultyName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.batchName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, r.startTime() != null ? r.startTime().format(TIME_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, r.endTime() != null ? r.endTime().format(TIME_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(r.termInstanceLabel()), dataFont, rowBg, Element.ALIGN_LEFT);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        }
    }

    // ── Occurrence-based (Class Schedules date-wise browser) ───────────────────

    public byte[] toExcelOccurrences(List<ClassScheduleOccurrenceResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Class Schedules");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, OCCURRENCE_HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, OCCURRENCE_HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                ClassScheduleOccurrenceResponse occ = rows.get(i);
                ClassScheduleResponse r = occ.session();
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                ExcelExportUtil.setCell(row, 0, String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1, occ.date().format(DATE_FMT), style);
                ExcelExportUtil.setCell(row, 2, r.sessionType() != null ? r.sessionType().name() : "—", style);
                ExcelExportUtil.setCell(row, 3, nvl(r.roomName()), style);
                ExcelExportUtil.setCell(row, 4, nvl(r.subjectName()), style);
                ExcelExportUtil.setCell(row, 5, nvl(r.subjectCode()), style);
                ExcelExportUtil.setCell(row, 6, nvl(r.facultyName()), style);
                ExcelExportUtil.setCell(row, 7, nvl(r.batchName()), style);
                ExcelExportUtil.setCell(row, 8, r.startTime() != null ? r.startTime().format(TIME_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 9, r.endTime() != null ? r.endTime().format(TIME_FMT) : "—", style);
                ExcelExportUtil.setCell(row, 10, occ.occurrenceStatus().name(), style);
            }

            int[] widths = { 6, 14, 12, 20, 24, 12, 22, 16, 12, 12, 14 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] toPdfOccurrences(List<ClassScheduleOccurrenceResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new java.awt.Color(255, 255, 255));
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 3, 9, 8, 14, 16, 8, 15, 11, 8, 8, 10 };
            PdfPTable table = PdfExportUtil.createHeaderTable(OCCURRENCE_HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                ClassScheduleOccurrenceResponse occ = rows.get(i);
                ClassScheduleResponse r = occ.session();
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, occ.date().format(DATE_FMT), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, r.sessionType() != null ? r.sessionType().name() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.roomName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.subjectName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.subjectCode()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.facultyName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.batchName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, r.startTime() != null ? r.startTime().format(TIME_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, r.endTime() != null ? r.endTime().format(TIME_FMT) : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, occ.occurrenceStatus().name(), dataFont, rowBg, Element.ALIGN_LEFT);
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
