package com.cms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.DashboardSummaryResponse;
import com.cms.dto.DashboardTrendsResponse;
import com.cms.service.DashboardService;

/**
 * REST controller serving the aggregated dashboard summary and trend data.
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY') or hasRole('ROLE_LAB_INCHARGE')")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/trends")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY') or hasRole('ROLE_LAB_INCHARGE')")
    public ResponseEntity<DashboardTrendsResponse> getTrends() {
        return ResponseEntity.ok(dashboardService.getTrends());
    }
}

