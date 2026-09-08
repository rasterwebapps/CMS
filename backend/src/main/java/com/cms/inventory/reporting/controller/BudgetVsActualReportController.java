package com.cms.inventory.reporting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.reporting.dto.BudgetVsActualReportResponse;
import com.cms.inventory.reporting.service.BudgetVsActualReportService;

/**
 * Reuses {@code INVENTORY_BUDGET_VIEW}/{@code _MANAGE} rather than a new dedicated permission —
 * same reasoning as every other report slice this session: this stays within the one
 * already-permissioned Budget bounded context. See the "Budget vs. Actual Report slice"
 * decision-log entry.
 */
@RestController
@RequestMapping("/inventory/reporting/budget-vs-actual")
public class BudgetVsActualReportController {

    private final BudgetVsActualReportService reportService;

    public BudgetVsActualReportController(BudgetVsActualReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_BUDGET_VIEW', 'INVENTORY_BUDGET_MANAGE')")
    public ResponseEntity<BudgetVsActualReportResponse> get() {
        return ResponseEntity.ok(reportService.get());
    }
}
