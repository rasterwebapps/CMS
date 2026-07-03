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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.StaffReferrerRequest;
import com.cms.dto.StaffReferrerResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.service.StaffReferrerExportService;
import com.cms.service.StaffReferrerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/staff-referrers")
public class StaffReferrerController {

    private final StaffReferrerService       service;
    private final StaffReferrerExportService exportService;

    public StaffReferrerController(StaffReferrerService service, StaffReferrerExportService exportService) {
        this.service       = service;
        this.exportService = exportService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<StaffReferrerResponse> create(@Valid @RequestBody StaffReferrerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<StaffReferrerResponse>> findAll(
            @RequestParam(required = false) Boolean active) {
        List<StaffReferrerResponse> list = Boolean.TRUE.equals(active)
            ? service.findActive()
            : service.findAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffReferrerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<StaffReferrerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StaffReferrerRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<StaffReferrerResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(service.deactivate(id));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<StaffReferrerResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(service.reactivate(id));
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('STAFF_REFERRER_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search) {
        List<StaffReferrerResponse> data = service.findAll(search);
        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = exportService.toPdf(data);
                String filename = "staff-referrers-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = exportService.toExcel(data);
                String filename = "staff-referrers-" + LocalDate.now() + ".xlsx";
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

    @GetMapping("/page")
    public ResponseEntity<Page<StaffReferrerResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.findPage(search, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam Long institutionId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(service.nameExists(value, institutionId, excludeId));
    }

    @GetMapping("/employee-code-exists")
    @PreAuthorize("@perm.has('STAFF_REFERRER_MANAGE')")
    public ResponseEntity<Boolean> employeeCodeExists(
            @RequestParam String value,
            @RequestParam Long institutionId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(service.employeeCodeExists(value, institutionId, excludeId));
    }
}
