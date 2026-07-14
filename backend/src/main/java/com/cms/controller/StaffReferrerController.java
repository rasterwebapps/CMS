package com.cms.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
import com.cms.util.ExportSortUtils;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.ExportResponseFactory;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/staff-referrers")
public class StaffReferrerController {

    private static final Map<String, String> EXPORT_SORT_FIELDS = new LinkedHashMap<>();
    static {
        EXPORT_SORT_FIELDS.put("name", "Name");
        EXPORT_SORT_FIELDS.put("employeeCode", "Employee Code");
        EXPORT_SORT_FIELDS.put("phone", "Phone");
        EXPORT_SORT_FIELDS.put("institution.name", "Institution");
        EXPORT_SORT_FIELDS.put("commissionAmount", "Commission Amount");
        EXPORT_SORT_FIELDS.put("isActive", "Active");
    }

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
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {

        Sort exportSort = ExportSortUtils.resolve(
            sort, direction, EXPORT_SORT_FIELDS.keySet(), "name", Sort.Direction.ASC);
        List<StaffReferrerResponse> data = service.findAll(search, exportSort);

        Sort.Order order = ExportSortUtils.firstOrder(exportSort, "name", Sort.Direction.ASC);
        ExportMetadata meta = ExportMetadata.of("Staff Referrers Export")
            .filter("Search", search)
            .sort(EXPORT_SORT_FIELDS.get(order.getProperty()), order.getDirection());

        return ExportResponseFactory.respond(format, "staff-referrers",
            () -> exportService.toExcel(data, meta),
            () -> exportService.toPdf(data, meta));
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
