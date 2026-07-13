package com.cms.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.LibraryBarcodeLabelsRequest;
import com.cms.dto.LibraryBookBulkTransferRequest;
import com.cms.dto.LibraryBookRequest;
import com.cms.dto.LibraryBookResponse;
import com.cms.dto.LibraryBookShelfTransferResponse;
import com.cms.dto.LibraryBookTransferRequest;
import com.cms.dto.LibraryBookTransferResult;
import com.cms.dto.LibraryPrinterActionResponse;
import com.cms.model.enums.BookStatus;
import com.cms.service.LibraryBarcodeService;
import com.cms.service.LibraryBookExportService;
import com.cms.service.LibraryBookService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/library/books")
public class LibraryBookController {

    private static final Logger log = LoggerFactory.getLogger(LibraryBookController.class);

    private final LibraryBookService bookService;
    private final LibraryBookExportService bookExportService;
    private final LibraryBarcodeService barcodeService;

    public LibraryBookController(LibraryBookService bookService,
                                  LibraryBookExportService bookExportService,
                                  LibraryBarcodeService barcodeService) {
        this.bookService = bookService;
        this.bookExportService = bookExportService;
        this.barcodeService = barcodeService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<LibraryBookResponse> create(@Valid @RequestBody LibraryBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.create(request));
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<List<LibraryBookResponse>> findAll(
            @RequestParam(required = false) BookStatus status) {
        if (status != null) {
            return ResponseEntity.ok(bookService.findByStatus(status));
        }
        return ResponseEntity.ok(bookService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<LibraryBookResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.findById(id));
    }

    @GetMapping("/accession-number-exists")
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<Map<String, Boolean>> accessionNumberExists(
            @RequestParam String accessionNumber,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = bookService.accessionNumberExists(accessionNumber, excludeId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/barcode-exists")
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<Map<String, Boolean>> barcodeExists(
            @RequestParam String barcode,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = bookService.barcodeExists(barcode, excludeId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<LibraryBookResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LibraryBookRequest request) {
        return ResponseEntity.ok(bookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<Page<LibraryBookResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BookStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long rackId,
            @RequestParam(required = false) Long shelfId,
            @PageableDefault(size = 25, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(bookService.findPage(search, status, category, rackId, shelfId, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BookStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long rackId,
            @RequestParam(required = false) Long shelfId) {

        List<LibraryBookResponse> data = bookService.findAllMatching(
            search, status, category, rackId, shelfId, Sort.by("title").ascending());

        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = bookExportService.toPdf(data);
                String filename = "book-catalogue-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = bookExportService.toExcel(data);
                String filename = "book-catalogue-" + LocalDate.now() + ".xlsx";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/barcode.png")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_PRINT_BARCODE')")
    public ResponseEntity<byte[]> barcodePng(@PathVariable Long id) {
        LibraryBookResponse book = bookService.findById(id);
        String code = book.barcode() != null ? book.barcode() : book.accessionNumber();
        try {
            byte[] png = barcodeService.generateBarcodePng(code);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
        } catch (Exception e) {
            log.error("Failed to generate barcode PNG for book id={} code={}", id, code, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/barcode-labels")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_PRINT_BARCODE')")
    public ResponseEntity<byte[]> barcodeLabels(@Valid @RequestBody LibraryBarcodeLabelsRequest request) {
        List<LibraryBarcodeService.LabelItem> items = request.ids().stream()
            .map(bookService::findById).map(this::toLabelItem).toList();

        try {
            byte[] bytes = barcodeService.generateLabelSheetPdf(items);
            String filename = "book-barcode-labels-" + LocalDate.now() + ".pdf";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to generate barcode label sheet for ids={}", request.ids(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/barcode.zpl")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_PRINT_BARCODE')")
    public ResponseEntity<String> barcodeZpl(@PathVariable Long id) {
        LibraryBookResponse book = bookService.findById(id);
        String zpl = barcodeService.generateZpl(toLabelItem(book));
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(zpl);
    }

    @PostMapping("/{id}/barcode-print")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_PRINT_BARCODE')")
    public ResponseEntity<LibraryPrinterActionResponse> barcodePrint(@PathVariable Long id) {
        LibraryBookResponse book = bookService.findById(id);
        String zpl = barcodeService.generateZpl(toLabelItem(book));
        try {
            barcodeService.sendZpl(zpl);
            return ResponseEntity.ok(new LibraryPrinterActionResponse(true, "Sent to printer"));
        } catch (Exception e) {
            log.error("Failed to send barcode ZPL to network printer for book id={}", id, e);
            return ResponseEntity.ok(new LibraryPrinterActionResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/barcode-labels.zpl")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_PRINT_BARCODE')")
    public ResponseEntity<String> barcodeLabelsZpl(@Valid @RequestBody LibraryBarcodeLabelsRequest request) {
        List<LibraryBarcodeService.LabelItem> items = request.ids().stream()
            .map(bookService::findById).map(this::toLabelItem).toList();
        String zpl = barcodeService.generateZplLabelSheet(items);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(zpl);
    }

    @PostMapping("/barcode-labels-print")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_PRINT_BARCODE')")
    public ResponseEntity<LibraryPrinterActionResponse> barcodeLabelsPrint(@Valid @RequestBody LibraryBarcodeLabelsRequest request) {
        List<LibraryBarcodeService.LabelItem> items = request.ids().stream()
            .map(bookService::findById).map(this::toLabelItem).toList();
        String zpl = barcodeService.generateZplLabelSheet(items);
        try {
            barcodeService.sendZpl(zpl);
            return ResponseEntity.ok(new LibraryPrinterActionResponse(true, "Sent to printer"));
        } catch (Exception e) {
            log.error("Failed to send bulk barcode ZPL to network printer for ids={}", request.ids(), e);
            return ResponseEntity.ok(new LibraryPrinterActionResponse(false, e.getMessage()));
        }
    }

    private LibraryBarcodeService.LabelItem toLabelItem(LibraryBookResponse book) {
        String code = book.barcode() != null ? book.barcode() : book.accessionNumber();
        return new LibraryBarcodeService.LabelItem(code, book.title(), book.accessionNumber());
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("@perm.has('LIBRARY_TRANSFER')")
    public ResponseEntity<LibraryBookShelfTransferResponse> transfer(
            @PathVariable Long id,
            @Valid @RequestBody LibraryBookTransferRequest request) {
        return ResponseEntity.ok(bookService.transferBook(id, request));
    }

    @PostMapping("/transfer/bulk")
    @PreAuthorize("@perm.has('LIBRARY_TRANSFER')")
    public ResponseEntity<LibraryBookTransferResult> bulkTransfer(
            @Valid @RequestBody LibraryBookBulkTransferRequest request) {
        return ResponseEntity.ok(bookService.bulkTransfer(request));
    }

    @GetMapping("/{id}/transfers")
    @PreAuthorize("@perm.hasAny('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE')")
    public ResponseEntity<List<LibraryBookShelfTransferResponse>> transferHistory(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getTransferHistory(id));
    }
}
