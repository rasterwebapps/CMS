package com.cms.inventory.consignment.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.consignment.dto.ConsignmentStockConsumeRequest;
import com.cms.inventory.consignment.dto.ConsignmentStockLineResponse;
import com.cms.inventory.consignment.dto.ConsignmentStockReceiveRequest;
import com.cms.inventory.consignment.service.ConsignmentStockLineService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/consignment/stock-lines")
public class ConsignmentStockLineController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_CONSIGNMENT_VIEW', 'INVENTORY_CONSIGNMENT_MANAGE', 'INVENTORY_CONSIGNMENT_CONVERT')";

    private final ConsignmentStockLineService lineService;

    public ConsignmentStockLineController(ConsignmentStockLineService lineService) {
        this.lineService = lineService;
    }

    @PostMapping("/receive")
    @PreAuthorize("@perm.has('INVENTORY_CONSIGNMENT_MANAGE')")
    public ResponseEntity<ConsignmentStockLineResponse> receive(@Valid @RequestBody ConsignmentStockReceiveRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lineService.receive(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<ConsignmentStockLineResponse>> findPage(
            @RequestParam(required = false) Long agreementId,
            @RequestParam(required = false) Long productId,
            @PageableDefault(size = 25, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(lineService.findPage(agreementId, productId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<ConsignmentStockLineResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(lineService.findById(id));
    }

    @PostMapping("/{id}/consume")
    @PreAuthorize("@perm.has('INVENTORY_CONSIGNMENT_CONVERT')")
    public ResponseEntity<ConsignmentStockLineResponse> consume(@PathVariable Long id, @Valid @RequestBody ConsignmentStockConsumeRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(lineService.consume(id, request, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
