package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

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

import com.cms.dto.FacultyRequest;
import com.cms.dto.FacultyResponse;
import com.cms.model.enums.FacultyStatus;
import com.cms.service.FacultyExportService;
import com.cms.service.FacultyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/faculty")
public class FacultyController {

    private final FacultyService       facultyService;
    private final FacultyExportService facultyExportService;

    public FacultyController(FacultyService facultyService, FacultyExportService facultyExportService) {
        this.facultyService       = facultyService;
        this.facultyExportService = facultyExportService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('FACULTY_MANAGE')")
    public ResponseEntity<FacultyResponse> create(@Valid @RequestBody FacultyRequest request) {
        FacultyResponse response = facultyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FacultyResponse>> findAll(
            @RequestParam(required = false) Long specialityId,
            @RequestParam(required = false) FacultyStatus status) {
        List<FacultyResponse> facultyList;
        if (specialityId != null) {
            facultyList = facultyService.findBySpecialityId(specialityId);
        } else if (status != null) {
            facultyList = facultyService.findByStatus(status);
        } else {
            facultyList = facultyService.findAll();
        }
        return ResponseEntity.ok(facultyList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyResponse> findById(@PathVariable Long id) {
        FacultyResponse response = facultyService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('FACULTY_MANAGE')")
    public ResponseEntity<FacultyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FacultyRequest request) {
        FacultyResponse response = facultyService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('FACULTY_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        facultyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/page")
    public ResponseEntity<Page<FacultyResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long specialityId,
            @RequestParam(required = false) FacultyStatus status,
            @RequestParam(required = false) String documentReview,
            @PageableDefault(size = 25, sort = "firstName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(facultyService.findPage(search, specialityId, status, documentReview, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('FACULTY_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long specialityId,
            @RequestParam(required = false) FacultyStatus status,
            @RequestParam(required = false) String documentReview) {
        List<FacultyResponse> data = facultyService.findAll(search, specialityId, status, documentReview);
        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = facultyExportService.toPdf(data);
                String filename = "faculty-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = facultyExportService.toExcel(data);
                String filename = "faculty-" + LocalDate.now() + ".xlsx";
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

    @GetMapping("/nrts-exists")
    public ResponseEntity<Boolean> nrtsExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(facultyService.nrtsNumberExists(value, excludeId));
    }

}
