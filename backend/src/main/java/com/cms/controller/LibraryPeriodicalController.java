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
import com.cms.dto.LibraryPeriodicalRequest;
import com.cms.dto.LibraryPeriodicalResponse;
import com.cms.dto.LibraryPrinterActionResponse;
import com.cms.model.enums.JournalType;
import com.cms.model.enums.SubscriptionStatus;
import com.cms.service.LibraryBarcodeService;
import com.cms.service.LibraryPeriodicalExportService;
import com.cms.service.LibraryPeriodicalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/library/periodicals")
public class LibraryPeriodicalController {

    private static final Logger log = LoggerFactory.getLogger(LibraryPeriodicalController.class);

    private final LibraryPeriodicalService periodicalService;
    private final LibraryPeriodicalExportService periodicalExportService;
    private final LibraryBarcodeService barcodeService;

    public LibraryPeriodicalController(LibraryPeriodicalService periodicalService,
                                        LibraryPeriodicalExportService periodicalExportService,
                                        LibraryBarcodeService barcodeService) {
        this.periodicalService = periodicalService;
        this.periodicalExportService = periodicalExportService;
        this.barcodeService = barcodeService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<LibraryPeriodicalResponse> create(
            @Valid @RequestBody LibraryPeriodicalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodicalService.create(request));
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('LIBRARY_PERIODICAL_VIEW', 'LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<List<LibraryPeriodicalResponse>> findAll(
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) JournalType journalType) {
        if (status != null) {
            return ResponseEntity.ok(periodicalService.findByStatus(status));
        }
        if (journalType != null) {
            return ResponseEntity.ok(periodicalService.findByType(journalType));
        }
        return ResponseEntity.ok(periodicalService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('LIBRARY_PERIODICAL_VIEW', 'LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<LibraryPeriodicalResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(periodicalService.findById(id));
    }

    @GetMapping("/accession-number-exists")
    @PreAuthorize("@perm.hasAny('LIBRARY_PERIODICAL_VIEW', 'LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<Map<String, Boolean>> accessionNumberExists(
            @RequestParam String accessionNumber,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = periodicalService.accessionNumberExists(accessionNumber, excludeId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/barcode-exists")
    @PreAuthorize("@perm.hasAny('LIBRARY_PERIODICAL_VIEW', 'LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<Map<String, Boolean>> barcodeExists(
            @RequestParam String barcode,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = periodicalService.barcodeExists(barcode, excludeId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<LibraryPeriodicalResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LibraryPeriodicalRequest request) {
        return ResponseEntity.ok(periodicalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        periodicalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('LIBRARY_PERIODICAL_VIEW', 'LIBRARY_PERIODICAL_MANAGE')")
    public ResponseEntity<Page<LibraryPeriodicalResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SubscriptionStatus subscriptionStatus,
            @RequestParam(required = false) JournalType journalType,
            @PageableDefault(size = 25, sort = "journalName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(periodicalService.findPage(search, subscriptionStatus, journalType, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SubscriptionStatus subscriptionStatus,
            @RequestParam(required = false) JournalType journalType) {

        List<LibraryPeriodicalResponse> data = periodicalService.findAllMatching(
            search, subscriptionStatus, journalType, Sort.by("journalName").ascending());

        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = periodicalExportService.toPdf(data);
                String filename = "journals-periodicals-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = periodicalExportService.toExcel(data);
                String filename = "journals-periodicals-" + LocalDate.now() + ".xlsx";
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
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_PRINT_BARCODE')")
    public ResponseEntity<byte[]> barcodePng(@PathVariable Long id) {
        LibraryPeriodicalResponse periodical = periodicalService.findById(id);
        String code = periodical.barcode() != null ? periodical.barcode() : periodical.accessionNumber();
        try {
            byte[] png = barcodeService.generateBarcodePng(code);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
        } catch (Exception e) {
            log.error("Failed to generate barcode PNG for periodical id={} code={}", id, code, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/barcode-labels")
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_PRINT_BARCODE')")
    public ResponseEntity<byte[]> barcodeLabels(@Valid @RequestBody LibraryBarcodeLabelsRequest request) {
        List<LibraryBarcodeService.LabelItem> items = request.ids().stream()
            .map(periodicalService::findById).map(this::toLabelItem).toList();

        try {
            byte[] bytes = barcodeService.generateLabelSheetPdf(items);
            String filename = "journal-barcode-labels-" + LocalDate.now() + ".pdf";
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
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_PRINT_BARCODE')")
    public ResponseEntity<String> barcodeZpl(@PathVariable Long id) {
        LibraryPeriodicalResponse periodical = periodicalService.findById(id);
        String zpl = barcodeService.generateZpl(toLabelItem(periodical));
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(zpl);
    }

    @PostMapping("/{id}/barcode-print")
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_PRINT_BARCODE')")
    public ResponseEntity<LibraryPrinterActionResponse> barcodePrint(@PathVariable Long id) {
        LibraryPeriodicalResponse periodical = periodicalService.findById(id);
        String zpl = barcodeService.generateZpl(toLabelItem(periodical));
        try {
            barcodeService.sendZpl(zpl);
            return ResponseEntity.ok(new LibraryPrinterActionResponse(true, "Sent to printer"));
        } catch (Exception e) {
            log.error("Failed to send barcode ZPL to network printer for periodical id={}", id, e);
            return ResponseEntity.ok(new LibraryPrinterActionResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/barcode-labels.zpl")
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_PRINT_BARCODE')")
    public ResponseEntity<String> barcodeLabelsZpl(@Valid @RequestBody LibraryBarcodeLabelsRequest request) {
        List<LibraryBarcodeService.LabelItem> items = request.ids().stream()
            .map(periodicalService::findById).map(this::toLabelItem).toList();
        String zpl = barcodeService.generateZplLabelSheet(items);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(zpl);
    }

    @PostMapping("/barcode-labels-print")
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_PRINT_BARCODE')")
    public ResponseEntity<LibraryPrinterActionResponse> barcodeLabelsPrint(@Valid @RequestBody LibraryBarcodeLabelsRequest request) {
        List<LibraryBarcodeService.LabelItem> items = request.ids().stream()
            .map(periodicalService::findById).map(this::toLabelItem).toList();
        String zpl = barcodeService.generateZplLabelSheet(items);
        try {
            barcodeService.sendZpl(zpl);
            return ResponseEntity.ok(new LibraryPrinterActionResponse(true, "Sent to printer"));
        } catch (Exception e) {
            log.error("Failed to send bulk barcode ZPL to network printer for ids={}", request.ids(), e);
            return ResponseEntity.ok(new LibraryPrinterActionResponse(false, e.getMessage()));
        }
    }

    private LibraryBarcodeService.LabelItem toLabelItem(LibraryPeriodicalResponse periodical) {
        String code = periodical.barcode() != null ? periodical.barcode() : periodical.accessionNumber();
        return new LibraryBarcodeService.LabelItem(code, periodical.journalName(), periodical.accessionNumber());
    }
}
