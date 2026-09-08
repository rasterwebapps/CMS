package com.cms.inventory.reporting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.reporting.dto.AssetDepreciationSummaryReportResponse;
import com.cms.inventory.reporting.service.AssetDepreciationSummaryReportService;

/**
 * Reuses {@code INVENTORY_ASSET_VIEW}/{@code _MANAGE} rather than a new dedicated permission —
 * same reasoning as the Stock Valuation and PO Aging reports: this stays within the one
 * already-permissioned Asset bounded context. See the "Asset Depreciation Summary Report slice"
 * decision-log entry.
 */
@RestController
@RequestMapping("/inventory/reporting/asset-depreciation-summary")
public class AssetDepreciationSummaryReportController {

    private final AssetDepreciationSummaryReportService reportService;

    public AssetDepreciationSummaryReportController(AssetDepreciationSummaryReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_ASSET_VIEW', 'INVENTORY_ASSET_MANAGE')")
    public ResponseEntity<AssetDepreciationSummaryReportResponse> get() {
        return ResponseEntity.ok(reportService.get());
    }
}
