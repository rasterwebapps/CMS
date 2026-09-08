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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.stock.dto.StockBalanceResponse;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.dto.StockMovementResponse;
import com.cms.inventory.stock.service.StockMovementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/stock")
public class StockController {

    private final StockMovementService stockMovementService;

    public StockController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @PostMapping("/movements")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_MANAGE')")
    public ResponseEntity<StockMovementResponse> recordMovement(
            @Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String performedBy = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(stockMovementService.recordMovement(request, performedBy));
    }

    @GetMapping("/balances/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_MANAGE')")
    public ResponseEntity<Page<StockBalanceResponse>> findBalancePage(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long locationId,
            @PageableDefault(size = 25, sort = "lastUpdated", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(stockMovementService.findBalancePage(productId, locationId, pageable));
    }
}
