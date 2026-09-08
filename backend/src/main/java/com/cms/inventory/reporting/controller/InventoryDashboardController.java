package com.cms.inventory.reporting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.reporting.dto.InventoryDashboardResponse;
import com.cms.inventory.reporting.service.InventoryDashboardService;

@RestController
@RequestMapping("/inventory/reporting/dashboard")
public class InventoryDashboardController {

    private final InventoryDashboardService dashboardService;

    public InventoryDashboardController(InventoryDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('INVENTORY_DASHBOARD_VIEW')")
    public ResponseEntity<InventoryDashboardResponse> get() {
        return ResponseEntity.ok(dashboardService.get());
    }
}
