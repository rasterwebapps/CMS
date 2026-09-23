package com.cms.inventory.procurement.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.DocumentNumberRegenerationResult;
import com.cms.inventory.procurement.dto.PurchaseOrderResponse;
import com.cms.inventory.procurement.dto.PurchaseRequisitionItemResponse;
import com.cms.inventory.procurement.dto.QuotationRequestAddLineRequest;
import com.cms.inventory.procurement.dto.QuotationRequestAddSupplierRequest;
import com.cms.inventory.procurement.dto.QuotationRequestAwardRequest;
import com.cms.inventory.procurement.dto.QuotationRequestCreateRequest;
import com.cms.inventory.procurement.dto.QuotationRequestLineResponse;
import com.cms.inventory.procurement.dto.QuotationRequestResponse;
import com.cms.inventory.procurement.dto.QuotationRequestSupplierResponse;
import com.cms.inventory.procurement.dto.QuotationResponseLineRequest;
import com.cms.inventory.procurement.dto.QuotationResponseLineResponse;
import com.cms.inventory.procurement.service.QuotationRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/procurement/quotation-requests")
public class QuotationRequestController {

    private static final String VIEW_ANY =
        "@perm.hasAny('INVENTORY_QUOTATION_VIEW', 'INVENTORY_QUOTATION_MANAGE', 'INVENTORY_QUOTATION_AWARD')";

    private final QuotationRequestService requestService;

    public QuotationRequestController(QuotationRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_MANAGE')")
    public ResponseEntity<QuotationRequestResponse> create(@Valid @RequestBody QuotationRequestCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<QuotationRequestResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(requestService.findPage(locationId, status, search, pageable));
    }

    @GetMapping("/regenerate-numbers/preview")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_REGENERATE_NUMBERS')")
    public ResponseEntity<DocumentNumberRegenerationResult> previewRegenerateNumbers() {
        return ResponseEntity.ok(requestService.regenerateQuotationNumbers(true));
    }

    @PostMapping("/regenerate-numbers")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_REGENERATE_NUMBERS')")
    public ResponseEntity<DocumentNumberRegenerationResult> regenerateNumbers() {
        return ResponseEntity.ok(requestService.regenerateQuotationNumbers(false));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<QuotationRequestResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.findById(id));
    }

    @GetMapping("/available-requisition-lines")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<List<PurchaseRequisitionItemResponse>> findAvailableRequisitionLines(@RequestParam Long locationId) {
        return ResponseEntity.ok(requestService.findAvailableRequisitionLines(locationId));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_MANAGE')")
    public ResponseEntity<QuotationRequestLineResponse> addLine(@PathVariable Long id, @Valid @RequestBody QuotationRequestAddLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.addLine(id, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_MANAGE')")
    public ResponseEntity<Void> removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        requestService.removeLine(id, lineId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/suppliers")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_MANAGE')")
    public ResponseEntity<QuotationRequestSupplierResponse> addSupplier(@PathVariable Long id, @Valid @RequestBody QuotationRequestAddSupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.addSupplier(id, request));
    }

    @DeleteMapping("/{id}/suppliers/{supplierId}")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_MANAGE')")
    public ResponseEntity<Void> removeSupplier(@PathVariable Long id, @PathVariable Long supplierId) {
        requestService.removeSupplier(id, supplierId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_MANAGE')")
    public ResponseEntity<QuotationRequestResponse> submit(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.submit(id, username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/responses/{supplierId}")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_MANAGE')")
    public ResponseEntity<QuotationResponseLineResponse> recordResponse(
            @PathVariable Long id, @PathVariable Long lineId, @PathVariable Long supplierId,
            @Valid @RequestBody QuotationResponseLineRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.recordResponse(id, lineId, supplierId, request, username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/award")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_AWARD')")
    public ResponseEntity<QuotationRequestLineResponse> award(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody QuotationRequestAwardRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.award(id, lineId, request, username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/reject")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_AWARD')")
    public ResponseEntity<QuotationRequestLineResponse> rejectLine(@PathVariable Long id, @PathVariable Long lineId) {
        return ResponseEntity.ok(requestService.rejectLine(id, lineId));
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_AWARD')")
    public ResponseEntity<List<PurchaseOrderResponse>> convertAwardedLines(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.convertAwardedLines(id, username(jwt)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('INVENTORY_QUOTATION_MANAGE')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        requestService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
