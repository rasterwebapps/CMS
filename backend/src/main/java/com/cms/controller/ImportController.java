package com.cms.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.ImportDefaultsRequest;
import com.cms.dto.ImportExecuteResult;
import com.cms.dto.ImportValidationResult;
import com.cms.service.ExcelTemplateService;
import com.cms.service.StudentImportService;

@RestController
@RequestMapping("/import")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
public class ImportController {

    private final ExcelTemplateService templateService;
    private final StudentImportService importService;

    public ImportController(ExcelTemplateService templateService,
                             StudentImportService importService) {
        this.templateService = templateService;
        this.importService   = importService;
    }

    /** Download the XLSX template pre-filled with reference data from this system. */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        byte[] bytes = templateService.generateTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("cms_import_template.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    /** Validate an uploaded file without writing to the database. */
    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportValidationResult> validate(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long defaultJoiningAcademicYearId,
            @RequestParam(required = false, defaultValue = "DAY_SCHOLAR") String defaultStudentType,
            @RequestParam(required = false, defaultValue = "Indian")   String defaultNationality,
            @RequestParam(required = false) String defaultState,
            @RequestParam(required = false, defaultValue = "1") Integer defaultSemester,
            @RequestParam(required = false, defaultValue = "false") Boolean skipErroredRows)
            throws Exception {
        ImportDefaultsRequest defaults = new ImportDefaultsRequest(
            defaultJoiningAcademicYearId,
            defaultStudentType, defaultNationality, defaultState,
            defaultSemester, skipErroredRows);
        return ResponseEntity.ok(importService.validate(file, defaults));
    }

    /** Validate and commit the import to the database. */
    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportExecuteResult> execute(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long defaultJoiningAcademicYearId,
            @RequestParam(required = false, defaultValue = "DAY_SCHOLAR") String defaultStudentType,
            @RequestParam(required = false, defaultValue = "Indian")   String defaultNationality,
            @RequestParam(required = false) String defaultState,
            @RequestParam(required = false, defaultValue = "1") Integer defaultSemester,
            @RequestParam(required = false, defaultValue = "false") Boolean skipErroredRows,
            @AuthenticationPrincipal Jwt jwt) throws Exception {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "import";
        ImportDefaultsRequest defaults = new ImportDefaultsRequest(
            defaultJoiningAcademicYearId,
            defaultStudentType, defaultNationality, defaultState,
            defaultSemester, skipErroredRows);
        return ResponseEntity.ok(importService.execute(file, defaults, username));
    }
}
