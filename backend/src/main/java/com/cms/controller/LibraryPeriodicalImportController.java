package com.cms.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.LibraryImportExecuteResult;
import com.cms.dto.LibraryImportValidationResult;
import com.cms.service.LibraryPeriodicalImportService;

@RestController
@RequestMapping("/library/periodicals/import")
@PreAuthorize("@perm.has('LIBRARY_PERIODICAL_IMPORT')")
public class LibraryPeriodicalImportController {

    private final LibraryPeriodicalImportService importService;

    public LibraryPeriodicalImportController(LibraryPeriodicalImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        byte[] bytes = importService.generateTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("library_journals_import_template.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LibraryImportValidationResult> validate(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "true") Boolean skipErroredRows)
            throws Exception {
        return ResponseEntity.ok(importService.validate(file, Boolean.TRUE.equals(skipErroredRows)));
    }

    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LibraryImportExecuteResult> execute(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "true") Boolean skipErroredRows)
            throws Exception {
        return ResponseEntity.ok(importService.execute(file, Boolean.TRUE.equals(skipErroredRows)));
    }
}
