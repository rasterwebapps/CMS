package com.cms.inventory.reporting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.reporting.dto.PurchaseOrderCycleTimeReportResponse;
import com.cms.inventory.reporting.service.PurchaseOrderCycleTimeReportService;

/**
 * Reuses {@code INVENTORY_PURCHASE_ORDER_VIEW}/{@code _MANAGE}, same reasoning as the PO Aging
 * Report: stays within the one already-permissioned Purchase Order bounded context. See the
 * "Purchase Order Cycle-Time Report slice" decision-log entry.
 */
@RestController
@RequestMapping("/inventory/reporting/purchase-order-cycle-time")
public class PurchaseOrderCycleTimeReportController {

    private final PurchaseOrderCycleTimeReportService reportService;

    public PurchaseOrderCycleTimeReportController(PurchaseOrderCycleTimeReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_PURCHASE_ORDER_VIEW', 'INVENTORY_PURCHASE_ORDER_MANAGE')")
    public ResponseEntity<PurchaseOrderCycleTimeReportResponse> get() {
        return ResponseEntity.ok(reportService.get());
    }
}
