package com.cms.inventory.stock.controller;

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

import com.cms.inventory.stock.dto.StockTransferAddLineRequest;
import com.cms.inventory.stock.dto.StockTransferCreateRequest;
import com.cms.inventory.stock.dto.StockTransferLineResponse;
import com.cms.inventory.stock.dto.StockTransferResponse;
import com.cms.inventory.stock.service.StockTransferService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/stock/transfers")
public class StockTransferController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_STOCK_TRANSFER_VIEW', 'INVENTORY_STOCK_TRANSFER_MANAGE')";

    private final StockTransferService transferService;

    public StockTransferController(StockTransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_STOCK_TRANSFER_MANAGE')")
    public ResponseEntity<StockTransferResponse> create(@Valid @RequestBody StockTransferCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<StockTransferResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transferService.findPage(locationId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<StockTransferResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.findById(id));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_TRANSFER_MANAGE')")
    public ResponseEntity<StockTransferLineResponse> addLine(@PathVariable Long id, @Valid @RequestBody StockTransferAddLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.addLine(id, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_TRANSFER_MANAGE')")
    public ResponseEntity<Void> removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        transferService.removeLine(id, lineId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_TRANSFER_MANAGE')")
    public ResponseEntity<StockTransferResponse> complete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(transferService.complete(id, username(jwt)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_TRANSFER_MANAGE')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        transferService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
