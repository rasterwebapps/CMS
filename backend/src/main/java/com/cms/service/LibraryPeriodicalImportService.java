package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.ImportRowError;
import com.cms.dto.LibraryImportExecuteResult;
import com.cms.dto.LibraryImportValidationResult;
import com.cms.model.LibraryPeriodical;
import com.cms.model.enums.BookStatus;
import com.cms.model.enums.JournalType;
import com.cms.model.enums.SubscriptionStatus;
import com.cms.repository.LibraryPeriodicalRepository;

@Service
public class LibraryPeriodicalImportService {

    private static final DateTimeFormatter DATE_FMT_DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_FMT_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Template column headers (must match parseRow order)
    private static final String[] HEADERS = {
        "Acc No (leave blank for auto)", "Journal Name*", "Journal Type (NATIONAL/INTERNATIONAL)",
        "Organization", "Volume Number", "Issue Number", "Month Range", "Year",
        "Subscription Status (ACTIVE/EXPIRED)", "Received Date (dd-MM-yyyy)", "Received By", "Remarks"
    };

    private final LibraryPeriodicalRepository periodicalRepository;
    private final LibraryAccessionRegistryService accessionRegistry;

    public LibraryPeriodicalImportService(LibraryPeriodicalRepository periodicalRepository,
                                           LibraryAccessionRegistryService accessionRegistry) {
        this.periodicalRepository = periodicalRepository;
        this.accessionRegistry = accessionRegistry;
    }

    // ── Template download ─────────────────────────────────────────

    public byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Journals");

            XSSFCellStyle headerStyle = buildHeaderStyle(wb);
            XSSFCellStyle sampleStyle = buildSampleStyle(wb);

            // Header row
            XSSFRow header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                XSSFCellStyle cs = wb.createCellStyle();
                cs.cloneStyleFrom(headerStyle);
                var cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(cs);
                sheet.setColumnWidth(i, 6000);
            }

            // Sample row
            XSSFRow sample = sheet.createRow(1);
            String[] sampleData = {
                "", "Indian Journal of Nursing", "NATIONAL",
                "Trained Nurses Association of India", "45", "3", "Jul-Sep", "2026",
                "ACTIVE", "01-07-2026", "Librarian", ""
            };
            for (int i = 0; i < sampleData.length; i++) {
                var cell = sample.createCell(i);
                cell.setCellValue(sampleData[i]);
                cell.setCellStyle(sampleStyle);
            }

            // Reference sheet
            XSSFSheet ref = wb.createSheet("Reference");
            ref.createRow(0).createCell(0).setCellValue("JOURNAL TYPE values");
            ref.createRow(1).createCell(0).setCellValue("NATIONAL");
            ref.createRow(2).createCell(0).setCellValue("INTERNATIONAL");
            ref.createRow(4).createCell(0).setCellValue("SUBSCRIPTION STATUS values");
            ref.createRow(5).createCell(0).setCellValue("ACTIVE");
            ref.createRow(6).createCell(0).setCellValue("EXPIRED");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Validate ──────────────────────────────────────────────────

    public LibraryImportValidationResult validate(MultipartFile file, boolean skipErroredRows) throws Exception {
        List<ImportRowError> errors   = new ArrayList<>();
        List<ImportRowError> warnings = new ArrayList<>();
        int totalRows = 0;
        int validRows = 0;

        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = wb.getSheet("Journals");
            if (sheet == null) {
                errors.add(new ImportRowError("Journals", 0, "Sheet", "No sheet named 'Journals' found in the uploaded file", "ERROR"));
                return new LibraryImportValidationResult(0, 0, 0, errors, warnings);
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                totalRows++;

                List<ImportRowError> rowErrors = validateRow(row, i + 1);
                errors.addAll(rowErrors.stream().filter(e -> "ERROR".equals(e.severity())).toList());
                warnings.addAll(rowErrors.stream().filter(e -> "WARNING".equals(e.severity())).toList());
                if (rowErrors.stream().noneMatch(e -> "ERROR".equals(e.severity()))) {
                    validRows++;
                }
            }
        }

        int invalidRows = totalRows - validRows;
        return new LibraryImportValidationResult(totalRows, validRows, invalidRows, errors, warnings);
    }

    // ── Execute ───────────────────────────────────────────────────

    @Transactional
    public LibraryImportExecuteResult execute(MultipartFile file, boolean skipErroredRows) throws Exception {
        List<ImportRowError> errors = new ArrayList<>();
        int imported = 0;
        int skipped  = 0;

        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = wb.getSheet("Journals");
            if (sheet == null) {
                errors.add(new ImportRowError("Journals", 0, "Sheet", "No sheet named 'Journals' found", "ERROR"));
                return new LibraryImportExecuteResult(0, 0, errors);
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                List<ImportRowError> rowErrors = validateRow(row, i + 1);
                boolean hasErrors = rowErrors.stream().anyMatch(e -> "ERROR".equals(e.severity()));

                if (hasErrors) {
                    if (skipErroredRows) {
                        errors.addAll(rowErrors);
                        skipped++;
                        continue;
                    } else {
                        errors.addAll(rowErrors);
                        throw new IllegalStateException("Import aborted at row " + (i + 1) + " — errors found");
                    }
                }

                LibraryPeriodical periodical = parseRow(row, i + 1, errors);
                if (periodical != null) {
                    periodicalRepository.save(periodical);
                    imported++;
                } else {
                    skipped++;
                }
            }
        }

        return new LibraryImportExecuteResult(imported, skipped, errors);
    }

    // ── Row parsing ───────────────────────────────────────────────

    private List<ImportRowError> validateRow(Row row, int displayRow) {
        List<ImportRowError> errors = new ArrayList<>();

        String journalName = str(row, 1);
        if (journalName.isBlank()) {
            errors.add(new ImportRowError("Journals", displayRow, "Journal Name", "Journal name is required", "ERROR"));
        }

        // Accession number uniqueness check
        String accNo = str(row, 0);
        if (!accNo.isBlank() && accessionRegistry.exists(accNo, null, null)) {
            errors.add(new ImportRowError("Journals", displayRow, "Acc No", "Accession number '" + accNo + "' already exists", "ERROR"));
        }

        // Journal type validation
        String type = str(row, 2);
        if (!type.isBlank()) {
            try {
                JournalType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new ImportRowError("Journals", displayRow, "Journal Type", "Invalid type '" + type + "'. Use NATIONAL or INTERNATIONAL", "ERROR"));
            }
        }

        // Year validation
        String yearStr = str(row, 7);
        if (!yearStr.isBlank()) {
            try { Integer.parseInt(yearStr); }
            catch (NumberFormatException e) {
                errors.add(new ImportRowError("Journals", displayRow, "Year", "Invalid year value '" + yearStr + "'", "WARNING"));
            }
        }

        // Subscription status validation
        String status = str(row, 8);
        if (!status.isBlank()) {
            try {
                SubscriptionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new ImportRowError("Journals", displayRow, "Subscription Status", "Invalid status '" + status + "'. Use ACTIVE or EXPIRED", "ERROR"));
            }
        }

        // Date format validation
        String receivedDate = str(row, 9);
        if (!receivedDate.isBlank()) {
            parseDate(receivedDate).ifPresentOrElse(d -> {}, () ->
                errors.add(new ImportRowError("Journals", displayRow, "Received Date", "Invalid date format '" + receivedDate + "'. Use dd-MM-yyyy", "WARNING")));
        }

        return errors;
    }

    private LibraryPeriodical parseRow(Row row, int displayRow, List<ImportRowError> errors) {
        LibraryPeriodical p = new LibraryPeriodical();

        String accNo = str(row, 0);
        p.setAccessionNumber(accessionRegistry.resolveAccessionNumber(accNo.isBlank() ? null : accNo));

        p.setJournalName(blankToNull(str(row, 1)));

        String type = str(row, 2).toUpperCase();
        if (!type.isBlank()) {
            try { p.setJournalType(JournalType.valueOf(type)); }
            catch (IllegalArgumentException ignored) {}
        }

        p.setOrganization(blankToNull(str(row, 3)));
        p.setVolumeNumber(blankToNull(str(row, 4)));
        p.setIssueNumber(blankToNull(str(row, 5)));
        p.setMonthRange(blankToNull(str(row, 6)));

        String yearStr = str(row, 7);
        if (!yearStr.isBlank()) {
            try { p.setYear(Integer.parseInt(yearStr)); }
            catch (NumberFormatException e) {
                errors.add(new ImportRowError("Journals", displayRow, "Year", "Invalid year value '" + yearStr + "'", "WARNING"));
            }
        }

        String status = str(row, 8).toUpperCase();
        if (!status.isBlank()) {
            try { p.setSubscriptionStatus(SubscriptionStatus.valueOf(status)); }
            catch (IllegalArgumentException ignored) {}
        }

        parseDate(str(row, 9)).ifPresent(p::setReceivedDate);
        p.setReceivedBy(blankToNull(str(row, 10)));
        p.setRemarks(blankToNull(str(row, 11)));
        p.setStatus(BookStatus.AVAILABLE);
        return p;
    }

    // ── Utilities ─────────────────────────────────────────────────

    private static String str(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            double d = cell.getNumericCellValue();
            long l = (long) d;
            return d == l ? String.valueOf(l) : String.valueOf(d);
        }
        return cell.getStringCellValue().trim();
    }

    private static java.util.Optional<LocalDate> parseDate(String s) {
        if (s == null || s.isBlank()) return java.util.Optional.empty();
        for (DateTimeFormatter fmt : List.of(DATE_FMT_DMY, DATE_FMT_ISO)) {
            try { return java.util.Optional.of(LocalDate.parse(s, fmt)); }
            catch (DateTimeParseException ignored) {}
        }
        return java.util.Optional.empty();
    }

    private static boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !str(row, c).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private XSSFCellStyle buildHeaderStyle(XSSFWorkbook wb) {
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        XSSFCellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private XSSFCellStyle buildSampleStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
