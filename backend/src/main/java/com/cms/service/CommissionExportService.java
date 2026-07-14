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

import com.cms.dto.CommissionExplorerResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

@Service
public class CommissionExportService {

    private static final NumberFormat INR = NumberFormat.getNumberInstance(new Locale("en", "IN"));

    static {
        INR.setMinimumFractionDigits(2);
        INR.setMaximumFractionDigits(2);
    }

    private static final List<String> HEADERS = List.of(
        "#", "Student Name", "Admission No.", "Program", "Course",
        "Referrer", "Source", "Enquiry Date",
        "Commission (₹)", "Paid (₹)", "Outstanding (₹)", "Status");

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<CommissionExplorerResponse> rows, ExportMetadata meta) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Commissions");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, HEADERS.size());
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, HEADERS);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                CommissionExplorerResponse r = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();

                ExcelExportUtil.setCell(row, 0,  String.valueOf(i + 1), style);
                ExcelExportUtil.setCell(row, 1,  nvl(r.studentName()), style);
                ExcelExportUtil.setCell(row, 2,  nvl(r.admissionNumber()), style);
                ExcelExportUtil.setCell(row, 3,  nvl(r.programName()), style);
                ExcelExportUtil.setCell(row, 4,  nvl(r.courseName()), style);
                ExcelExportUtil.setCell(row, 5,  referrer(r), style);
                ExcelExportUtil.setCell(row, 6,  nvl(r.commissionSource()), style);
                ExcelExportUtil.setCell(row, 7,  nvl(r.enquiryDate()), style);
                ExcelExportUtil.setCell(row, 8,  fmtInr(r.commissionAmount()), style);
                ExcelExportUtil.setCell(row, 9,  fmtInr(r.commissionPaidAmount()), style);
                ExcelExportUtil.setCell(row, 10, fmtInr(r.commissionOutstanding()), style);
                ExcelExportUtil.setCell(row, 11, nvl(r.commissionPaymentStatus()), style);
            }

            int[] widths = { 5, 26, 16, 18, 18, 22, 14, 14, 16, 14, 16, 14 };
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<CommissionExplorerResponse> rows, ExportMetadata meta) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new java.awt.Color(255, 255, 255));
            Font dataFont   = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = { 3, 14, 9, 10, 10, 12, 8, 8, 9, 8, 9, 8 };
            PdfPTable table = PdfExportUtil.createHeaderTable(HEADERS, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                CommissionExplorerResponse r = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                PdfExportUtil.addCell(table, String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, nvl(r.studentName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.admissionNumber()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.programName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.courseName()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, referrer(r), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.commissionSource()), dataFont, rowBg, Element.ALIGN_LEFT);
                PdfExportUtil.addCell(table, nvl(r.enquiryDate()), dataFont, rowBg, Element.ALIGN_CENTER);
                PdfExportUtil.addCell(table, fmtInr(r.commissionAmount()), dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, fmtInr(r.commissionPaidAmount()), dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, fmtInr(r.commissionOutstanding()), dataFont, rowBg, Element.ALIGN_RIGHT);
                PdfExportUtil.addCell(table, nvl(r.commissionPaymentStatus()), dataFont, rowBg, Element.ALIGN_LEFT);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String referrer(CommissionExplorerResponse r) {
        if (r.agentName() != null && !r.agentName().isBlank())             return r.agentName();
        if (r.staffReferrerName() != null && !r.staffReferrerName().isBlank()) return r.staffReferrerName();
        if (r.referredFacultyName() != null && !r.referredFacultyName().isBlank()) return r.referredFacultyName();
        if (r.referralTypeName() != null && !r.referralTypeName().isBlank()) return r.referralTypeName();
        return "—";
    }

    private static String fmtInr(BigDecimal val) {
        if (val == null) return "0.00";
        return INR.format(val);
    }

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
