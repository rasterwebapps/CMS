package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.cms.dto.LibraryBookResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Book Catalogue export — a true reflection of whatever the user currently has on screen.
 * Column set is driven entirely by the caller's {@code columns} list (the Book Explorer
 * column picker's visible/ordered keys), not a fixed layout, so this doubles as the
 * Accession Register export once the picker's register-only columns are turned on.
 */
@Service
public class LibraryBookExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /** Used only when the caller supplies no (or an entirely unrecognized) column list. */
    public static final List<String> DEFAULT_COLUMNS = List.of(
        "accessionNumber", "title", "authors", "publisher", "shelf", "callNumber", "status");

    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    private static final Map<String, Function<LibraryBookResponse, String>> RESOLVERS = new LinkedHashMap<>();
    private static final Map<String, Integer> WIDTHS = new LinkedHashMap<>();

    private static void register(String key, String label, int width, Function<LibraryBookResponse, String> resolver) {
        LABELS.put(key, label);
        RESOLVERS.put(key, resolver);
        WIDTHS.put(key, width);
    }

    static {
        register("accessionNumber",   "Acc. No.",         16, b -> nvl(b.accessionNumber()));
        register("title",             "Title",            34, b -> nvl(b.title()));
        register("authors",           "Author(s)",        24, b -> nvl(b.authors()));
        register("publisher",         "Publisher",        22, b -> nvl(b.publisher()));
        register("shelf",             "Shelf",            18, LibraryBookExportService::shelfLabel);
        register("callNumber",        "Call No.",         16, b -> nvl(b.callNumber()));
        register("status",            "Status",           14, b -> b.status() != null ? b.status().name() : "—");
        register("entryDate",         "Entry Date",       14, b -> b.entryDate() != null ? b.entryDate().format(DATE_FMT) : "—");
        register("isbn",              "ISBN",             16, b -> nvl(b.isbn()));
        register("edition",           "Edition",          12, b -> nvl(b.edition()));
        register("yearOfPublication", "Year",             10, b -> nvl(b.yearOfPublication()));
        register("collation",         "Collation",        16, b -> nvl(b.collation()));
        register("series",            "Series",           16, b -> nvl(b.series()));
        register("subjectCategory",   "Subject",           18, b -> nvl(b.subjectCategory()));
        register("vendorDonorName",   "Source / Vendor",  20, b -> nvl(b.vendorDonorName()));
        register("billNumber",        "Bill No. & Date",  20, LibraryBookExportService::billLabel);
        register("priceRs",           "Price (Rs.)",      12, b -> b.priceRs() != null ? b.priceRs().toString() : "—");
        register("remarks",           "Remarks",          24, b -> nvl(b.remarks()));
    }

    /** Resolves requested column keys against the known set, falling back to the default catalogue columns. */
    public static List<String> resolveColumns(List<String> requested) {
        if (requested == null || requested.isEmpty()) return DEFAULT_COLUMNS;
        List<String> valid = requested.stream().filter(LABELS::containsKey).toList();
        return valid.isEmpty() ? DEFAULT_COLUMNS : valid;
    }

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<LibraryBookResponse> rows, List<String> columns) throws IOException {
        List<String> cols = resolveColumns(columns);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Book Catalogue");

            XSSFCellStyle titleStyle = wb.createCellStyle();
            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            XSSFCellStyle dataStyle = wb.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.HAIR);

            XSSFCellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            altStyle.setBorderBottom(BorderStyle.THIN);
            altStyle.setBorderRight(BorderStyle.HAIR);

            XSSFRow titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(22);
            var titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Book Catalogue Export");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, cols.size() - 1));

            XSSFRow headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(18);
            for (int i = 0; i < cols.size(); i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(LABELS.get(cols.get(i)));
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 2;
            for (LibraryBookResponse b : rows) {
                XSSFRow row = sheet.createRow(rowIdx);
                XSSFCellStyle style = (rowIdx % 2 == 0) ? dataStyle : altStyle;
                for (int i = 0; i < cols.size(); i++) {
                    setCell(row, i, RESOLVERS.get(cols.get(i)).apply(b), style);
                }
                rowIdx++;
            }

            for (int i = 0; i < cols.size(); i++) {
                sheet.setColumnWidth(i, WIDTHS.getOrDefault(cols.get(i), 18) * 256);
            }
            sheet.createFreezePane(0, 2);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<LibraryBookResponse> rows, List<String> columns) throws IOException {
        List<String> cols = resolveColumns(columns);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8,
                new java.awt.Color(255, 255, 255));
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            Paragraph title = new Paragraph("Book Catalogue Export", titleFont);
            title.setSpacingAfter(10);
            doc.add(title);

            PdfPTable table = new PdfPTable(cols.size());
            table.setWidthPercentage(100);
            float[] colWidths = new float[cols.size()];
            for (int i = 0; i < cols.size(); i++) colWidths[i] = WIDTHS.getOrDefault(cols.get(i), 18);
            table.setWidths(colWidths);

            java.awt.Color headerBg = new java.awt.Color(13, 27, 62);
            java.awt.Color altBg = new java.awt.Color(235, 241, 255);

            for (String key : cols) {
                PdfPCell cell = new PdfPCell(new Phrase(LABELS.get(key), headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(4);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            int idx = 1;
            for (LibraryBookResponse b : rows) {
                java.awt.Color rowBg = (idx % 2 == 0) ? altBg : null;
                for (String key : cols) {
                    addCell(table, RESOLVERS.get(key).apply(b), dataFont, rowBg, Element.ALIGN_LEFT);
                }
                idx++;
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String shelfLabel(LibraryBookResponse b) {
        if (b.rackName() == null && b.shelfName() == null) return "—";
        return nvl(b.rackName()) + " / " + nvl(b.shelfName());
    }

    private static String billLabel(LibraryBookResponse b) {
        if (b.billNumber() == null && b.billDate() == null) return "—";
        String number = b.billNumber() != null ? b.billNumber() : "—";
        return b.billDate() != null ? number + " / " + b.billDate().format(DATE_FMT) : number;
    }

    private static void setCell(XSSFRow row, int col, String value, XSSFCellStyle style) {
        var cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void addCell(PdfPTable table, String text, Font font,
                                 java.awt.Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (bg != null) cell.setBackgroundColor(bg);
        cell.setPadding(3);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
