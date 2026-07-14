package com.cms.util.export;

import java.util.List;
import java.util.Map;

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

/**
 * Shared Apache POI rendering pieces reused by every {@code *ExportService} — style
 * creation, the title/filter/sort metadata block, and the header row — so each service
 * only owns its own per-DTO data-row extraction and column layout.
 */
public final class ExcelExportUtil {

    private ExcelExportUtil() {}

    public record Styles(XSSFCellStyle title, XSSFCellStyle meta, XSSFCellStyle header,
                          XSSFCellStyle data, XSSFCellStyle alt) {}

    public static Styles createStyles(XSSFWorkbook wb) {
        XSSFCellStyle titleStyle = wb.createCellStyle();
        XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        XSSFCellStyle metaStyle = wb.createCellStyle();
        XSSFFont metaFont = wb.createFont();
        metaFont.setItalic(true);
        metaFont.setFontHeightInPoints((short) 10);
        metaFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        metaStyle.setFont(metaFont);

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

        return new Styles(titleStyle, metaStyle, headerStyle, dataStyle, altStyle);
    }

    /**
     * Writes the title row, then one merged row per active filter, then a sort row
     * (when present) — all merged across {@code columnCount} columns. Returns the row
     * index where the header row should be written next.
     */
    public static int writeMetadataBlock(XSSFSheet sheet, Styles styles, ExportMetadata meta, int columnCount) {
        int rowIdx = 0;

        XSSFRow titleRow = sheet.createRow(rowIdx);
        titleRow.setHeightInPoints(22);
        var titleCell = titleRow.createCell(0);
        titleCell.setCellValue(meta.title());
        titleCell.setCellStyle(styles.title());
        if (columnCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, columnCount - 1));
        }
        rowIdx++;

        for (Map.Entry<String, String> entry : meta.filters().entrySet()) {
            rowIdx = writeMetaLine(sheet, styles, rowIdx, entry.getKey() + ": " + entry.getValue(), columnCount);
        }

        if (meta.sortLabel() != null) {
            rowIdx = writeMetaLine(sheet, styles, rowIdx, "Sorted by: " + meta.sortLabel(), columnCount);
        }

        return rowIdx;
    }

    private static int writeMetaLine(XSSFSheet sheet, Styles styles, int rowIdx, String text, int columnCount) {
        XSSFRow row = sheet.createRow(rowIdx);
        var cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(styles.meta());
        if (columnCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, columnCount - 1));
        }
        return rowIdx + 1;
    }

    public static void writeHeaderRow(XSSFSheet sheet, Styles styles, int rowIdx, List<String> headers) {
        XSSFRow headerRow = sheet.createRow(rowIdx);
        headerRow.setHeightInPoints(18);
        for (int i = 0; i < headers.size(); i++) {
            var cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(styles.header());
        }
    }

    public static void applyColumnWidths(XSSFSheet sheet, int[] widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    public static void setCell(XSSFRow row, int col, String value, XSSFCellStyle style) {
        var cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}
