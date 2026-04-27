package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.TermBillingScheduleDto;
import com.cms.dto.TermBillingScheduleRequest;
import com.cms.model.enums.TermType;
import com.cms.service.TermBillingScheduleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/term-billing-schedules")
public class TermBillingScheduleController {

    private final TermBillingScheduleService service;

    public TermBillingScheduleController(TermBillingScheduleService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<TermBillingScheduleDto> createOrUpdate(
            @Valid @RequestBody TermBillingScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createOrUpdate(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<TermBillingScheduleDto> update(
            @PathVariable Long id,
            @Valid @RequestBody TermBillingScheduleRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping
    public ResponseEntity<List<TermBillingScheduleDto>> getByAcademicYear(
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(service.getAllForAcademicYear(academicYearId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TermBillingScheduleDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/late-fee")
    public ResponseEntity<java.math.BigDecimal> computeLateFee(
            @RequestParam Long academicYearId,
            @RequestParam TermType termType,
            @RequestParam LocalDate paymentDate) {
        return ResponseEntity.ok(service.computeLateFee(academicYearId, termType, paymentDate));
    }
}
