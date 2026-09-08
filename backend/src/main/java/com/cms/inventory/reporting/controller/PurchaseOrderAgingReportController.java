package com.cms.inventory.reporting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.reporting.dto.PurchaseOrderAgingReportResponse;
import com.cms.inventory.reporting.service.PurchaseOrderAgingReportService;

/**
 * Reuses {@code INVENTORY_PURCHASE_ORDER_VIEW}/{@code _MANAGE} rather than a new dedicated
 * permission — same reasoning as the Stock Valuation Report: this stays within the one
 * already-permissioned Purchase Order bounded context. See the "Purchase Order Aging Report
 * slice" decision-log entry.
 */
@RestController
@RequestMapping("/inventory/reporting/purchase-order-aging")
public class PurchaseOrderAgingReportController {

    private final PurchaseOrderAgingReportService reportService;

    public PurchaseOrderAgingReportController(PurchaseOrderAgingReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_PURCHASE_ORDER_VIEW', 'INVENTORY_PURCHASE_ORDER_MANAGE')")
    public ResponseEntity<PurchaseOrderAgingReportResponse> get() {
        return ResponseEntity.ok(reportService.get());
    }
}
