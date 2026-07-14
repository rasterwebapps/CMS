package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.cms.dto.LibraryBookResponse;
import com.cms.util.export.ExcelExportUtil;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.PdfExportUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPTable;

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

    /** Column-key -> display label, for building a sort-order metadata line from a sort key (e.g. "title" -> "Title"). */
    public static String columnLabel(String key) {
        return LABELS.getOrDefault(key, key);
    }

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(List<LibraryBookResponse> rows, List<String> columns, ExportMetadata meta) throws IOException {
        List<String> cols = resolveColumns(columns);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Book Catalogue");
            ExcelExportUtil.Styles styles = ExcelExportUtil.createStyles(wb);

            int headerRowIdx = ExcelExportUtil.writeMetadataBlock(sheet, styles, meta, cols.size());
            List<String> headerLabels = cols.stream().map(LABELS::get).toList();
            ExcelExportUtil.writeHeaderRow(sheet, styles, headerRowIdx, headerLabels);

            int dataStart = headerRowIdx + 1;
            for (int i = 0; i < rows.size(); i++) {
                LibraryBookResponse b = rows.get(i);
                XSSFRow row = sheet.createRow(dataStart + i);
                XSSFCellStyle style = (i % 2 == 0) ? styles.data() : styles.alt();
                for (int c = 0; c < cols.size(); c++) {
                    ExcelExportUtil.setCell(row, c, RESOLVERS.get(cols.get(c)).apply(b), style);
                }
            }

            int[] widths = new int[cols.size()];
            for (int i = 0; i < cols.size(); i++) widths[i] = WIDTHS.getOrDefault(cols.get(i), 18);
            ExcelExportUtil.applyColumnWidths(sheet, widths);
            sheet.createFreezePane(0, dataStart);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    public byte[] toPdf(List<LibraryBookResponse> rows, List<String> columns, ExportMetadata meta) throws IOException {
        List<String> cols = resolveColumns(columns);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = PdfExportUtil.openLandscapeDocument(out, 30, 30, 40, 30);
            PdfExportUtil.writeTitleAndMetadata(doc, meta);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new java.awt.Color(255, 255, 255));
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            float[] colWidths = new float[cols.size()];
            for (int i = 0; i < cols.size(); i++) colWidths[i] = WIDTHS.getOrDefault(cols.get(i), 18);
            List<String> headerLabels = cols.stream().map(LABELS::get).toList();
            PdfPTable table = PdfExportUtil.createHeaderTable(headerLabels, colWidths, headerFont);

            for (int i = 0; i < rows.size(); i++) {
                LibraryBookResponse b = rows.get(i);
                java.awt.Color rowBg = (i % 2 == 0) ? PdfExportUtil.ALT_BG : null;
                for (String key : cols) {
                    PdfExportUtil.addCell(table, RESOLVERS.get(key).apply(b), dataFont, rowBg, Element.ALIGN_LEFT);
                }
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

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
