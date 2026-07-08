package com.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
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
import com.cms.dto.LibraryBookImportExecuteResult;
import com.cms.dto.LibraryBookImportValidationResult;
import com.cms.model.Library;
import com.cms.model.LibraryBook;
import com.cms.model.enums.BookSourceOfSupply;
import com.cms.model.enums.BookStatus;
import com.cms.repository.LibraryBookRepository;

@Service
public class LibraryBookImportService {

    private static final DateTimeFormatter DATE_FMT_DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_FMT_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Template column headers (must match parseRow order)
    private static final String[] HEADERS = {
        "Acc No (leave blank for auto)", "Entry Date (dd-MM-yyyy)", "Title*",
        "Authors*", "Publisher", "Year of Publication", "Edition", "ISBN",
        "Collation", "Series", "Call No", "Shelf Location", "Subject Category",
        "Source (PURCHASE/DONATION/EXCHANGE)", "Vendor / Donor Name",
        "Bill No", "Bill Date (dd-MM-yyyy)", "Price (Rs)", "Remarks"
    };

    private final LibraryBookRepository bookRepository;
    private final LibraryAccessionRegistryService accessionRegistry;
    private final LibraryService libraryService;
    private final LibraryShelfService shelfService;

    public LibraryBookImportService(LibraryBookRepository bookRepository,
                                     LibraryAccessionRegistryService accessionRegistry,
                                     LibraryService libraryService,
                                     LibraryShelfService shelfService) {
        this.bookRepository = bookRepository;
        this.accessionRegistry = accessionRegistry;
        this.libraryService = libraryService;
        this.shelfService = shelfService;
    }

    // ── Template download ─────────────────────────────────────────

    public byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Books");

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
                "", "01-01-2026", "Human Physiology",
                "Vander Sherman Luciano", "McGraw Hill", "1990", "5th Edition", "9780071009980",
                "800 pages", "", "612 VAN/H", "C1-R2", "Anatomy & Physiology",
                "PURCHASE", "Sri Krishna Book Store",
                "INV-001", "01-01-2026", "650.00", ""
            };
            for (int i = 0; i < sampleData.length; i++) {
                var cell = sample.createCell(i);
                cell.setCellValue(sampleData[i]);
                cell.setCellStyle(sampleStyle);
            }

            // Reference sheet
            XSSFSheet ref = wb.createSheet("Reference");
            ref.createRow(0).createCell(0).setCellValue("SOURCE values");
            ref.createRow(1).createCell(0).setCellValue("PURCHASE");
            ref.createRow(2).createCell(0).setCellValue("DONATION");
            ref.createRow(3).createCell(0).setCellValue("EXCHANGE");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Validate ──────────────────────────────────────────────────

    public LibraryBookImportValidationResult validate(MultipartFile file, boolean skipErroredRows) throws Exception {
        List<ImportRowError> errors   = new ArrayList<>();
        List<ImportRowError> warnings = new ArrayList<>();
        int totalRows = 0;
        int validRows = 0;

        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = wb.getSheet("Books");
            if (sheet == null) {
                errors.add(new ImportRowError("Books", 0, "Sheet", "No sheet named 'Books' found in the uploaded file", "ERROR"));
                return new LibraryBookImportValidationResult(0, 0, 0, errors, warnings);
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
        return new LibraryBookImportValidationResult(totalRows, validRows, invalidRows, errors, warnings);
    }

    // ── Execute ───────────────────────────────────────────────────

    @Transactional
    public LibraryBookImportExecuteResult execute(MultipartFile file, boolean skipErroredRows) throws Exception {
        List<ImportRowError> errors   = new ArrayList<>();
        int imported = 0;
        int skipped  = 0;
        Library library = libraryService.getDefault();

        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = wb.getSheet("Books");
            if (sheet == null) {
                errors.add(new ImportRowError("Books", 0, "Sheet", "No sheet named 'Books' found", "ERROR"));
                return new LibraryBookImportExecuteResult(0, 0, errors);
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

                LibraryBook book = parseRow(row, i + 1, errors, library);
                if (book != null) {
                    bookRepository.save(book);
                    imported++;
                } else {
                    skipped++;
                }
            }
        }

        return new LibraryBookImportExecuteResult(imported, skipped, errors);
    }

    // ── Row parsing ───────────────────────────────────────────────

    private List<ImportRowError> validateRow(Row row, int displayRow) {
        List<ImportRowError> errors = new ArrayList<>();

        String title = str(row, 2);
        if (title.isBlank()) {
            errors.add(new ImportRowError("Books", displayRow, "Title", "Title is required", "ERROR"));
        }
        String authors = str(row, 3);
        if (authors.isBlank()) {
            errors.add(new ImportRowError("Books", displayRow, "Authors", "Author(s) is required", "ERROR"));
        }

        // Accession number uniqueness check
        String accNo = str(row, 0);
        if (!accNo.isBlank() && accessionRegistry.exists(accNo, null, null)) {
            errors.add(new ImportRowError("Books", displayRow, "Acc No", "Accession number '" + accNo + "' already exists", "ERROR"));
        }

        // Source of supply validation
        String source = str(row, 13);
        if (!source.isBlank()) {
            try {
                BookSourceOfSupply.valueOf(source.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new ImportRowError("Books", displayRow, "Source", "Invalid source '" + source + "'. Use PURCHASE, DONATION, or EXCHANGE", "ERROR"));
            }
        }

        // Date format validation
        String entryDate = str(row, 1);
        if (!entryDate.isBlank()) {
            parseDate(entryDate).ifPresentOrElse(d -> {}, () ->
                errors.add(new ImportRowError("Books", displayRow, "Entry Date", "Invalid date format '" + entryDate + "'. Use dd-MM-yyyy", "WARNING")));
        }

        String billDate = str(row, 16);
        if (!billDate.isBlank()) {
            parseDate(billDate).ifPresentOrElse(d -> {}, () ->
                errors.add(new ImportRowError("Books", displayRow, "Bill Date", "Invalid date format '" + billDate + "'. Use dd-MM-yyyy", "WARNING")));
        }

        return errors;
    }

    private LibraryBook parseRow(Row row, int displayRow, List<ImportRowError> errors, Library library) {
        LibraryBook book = new LibraryBook();
        book.setLibrary(library);

        String accNo = str(row, 0);
        book.setAccessionNumber(accessionRegistry.resolveAccessionNumber(accNo.isBlank() ? null : accNo));

        parseDate(str(row, 1)).ifPresent(book::setEntryDate);
        book.setTitle(blankToNull(str(row, 2)));
        book.setAuthors(blankToNull(str(row, 3)));
        book.setPublisher(blankToNull(str(row, 4)));
        book.setYearOfPublication(blankToNull(str(row, 5)));
        book.setEdition(blankToNull(str(row, 6)));
        book.setIsbn(blankToNull(str(row, 7)));
        book.setCollation(blankToNull(str(row, 8)));
        book.setSeries(blankToNull(str(row, 9)));
        book.setCallNumber(blankToNull(str(row, 10)));
        book.setShelf(shelfService.resolveOrCreateFromLegacyText(library, str(row, 11)));
        book.setSubjectCategory(blankToNull(str(row, 12)));

        String source = str(row, 13).toUpperCase();
        if (!source.isBlank()) {
            try { book.setSourceOfSupply(BookSourceOfSupply.valueOf(source)); }
            catch (IllegalArgumentException ignored) {}
        }

        book.setVendorDonorName(blankToNull(str(row, 14)));
        book.setBillNumber(blankToNull(str(row, 15)));
        parseDate(str(row, 16)).ifPresent(book::setBillDate);

        String priceStr = str(row, 17);
        if (!priceStr.isBlank()) {
            try { book.setPriceRs(new BigDecimal(priceStr)); }
            catch (NumberFormatException e) {
                errors.add(new ImportRowError("Books", displayRow, "Price", "Invalid price value '" + priceStr + "'", "WARNING"));
            }
        }

        book.setRemarks(blankToNull(str(row, 18)));
        book.setStatus(BookStatus.AVAILABLE);
        return book;
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
