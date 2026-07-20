package com.cms.controller;

import java.util.List;

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

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.PeriodRequest;
import com.cms.dto.PeriodResponse;
import com.cms.service.PeriodService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/periods")
public class PeriodController {

    private final PeriodService periodService;

    public PeriodController(PeriodService periodService) {
        this.periodService = periodService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('PERIOD_MANAGE')")
    public ResponseEntity<PeriodResponse> create(@Valid @RequestBody PeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PeriodResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<PeriodResponse> result = activeOnly
            ? periodService.findActive()
            : periodService.findAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PeriodResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(periodService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('PERIOD_MANAGE')")
    public ResponseEntity<PeriodResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PeriodRequest request) {
        return ResponseEntity.ok(periodService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('PERIOD_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        periodService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('PERIOD_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(periodService.updateStatus(id, request));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<PeriodResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "periodOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(periodService.findPage(search, pageable));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('PERIOD_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(periodService.nameExists(value, excludeId));
    }
}
