package com.cms.inventory.consignment.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import com.cms.inventory.consignment.dto.ConsignmentAgreementRequest;
import com.cms.inventory.consignment.dto.ConsignmentAgreementResponse;
import com.cms.inventory.consignment.service.ConsignmentAgreementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/consignment/agreements")
public class ConsignmentAgreementController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_CONSIGNMENT_VIEW', 'INVENTORY_CONSIGNMENT_MANAGE', 'INVENTORY_CONSIGNMENT_CONVERT')";

    private final ConsignmentAgreementService agreementService;

    public ConsignmentAgreementController(ConsignmentAgreementService agreementService) {
        this.agreementService = agreementService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_CONSIGNMENT_MANAGE')")
    public ResponseEntity<ConsignmentAgreementResponse> create(@Valid @RequestBody ConsignmentAgreementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agreementService.create(request));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<ConsignmentAgreementResponse>> findPage(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 25, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(agreementService.findPage(supplierId, locationId, activeOnly, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<ConsignmentAgreementResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(agreementService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_CONSIGNMENT_MANAGE')")
    public ResponseEntity<ConsignmentAgreementResponse> update(@PathVariable Long id, @Valid @RequestBody ConsignmentAgreementRequest request) {
        return ResponseEntity.ok(agreementService.update(id, request));
    }
}
