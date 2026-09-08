package com.cms.inventory.reporting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.reporting.dto.StockValuationReportResponse;
import com.cms.inventory.reporting.service.StockValuationReportService;

/**
 * Reuses {@code INVENTORY_STOCK_VIEW}/{@code _MANAGE} rather than a new dedicated permission —
 * this report is a rollup of exactly the same {@code StockBalance} data the Stock Balance list
 * screen already shows under that permission, not a new operation crossing bounded contexts (the
 * way the Dashboard did, which is why that one got its own permission). See the "Stock Valuation
 * Report slice" decision-log entry.
 */
@RestController
@RequestMapping("/inventory/reporting/stock-valuation")
public class StockValuationReportController {

    private final StockValuationReportService reportService;

    public StockValuationReportController(StockValuationReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_MANAGE')")
    public ResponseEntity<StockValuationReportResponse> get(@RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(reportService.get(locationId));
    }
}
