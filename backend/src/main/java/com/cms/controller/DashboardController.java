package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.DashboardSummaryResponse;
import com.cms.dto.DashboardTrendsResponse;
import com.cms.dto.FrontOfficeDashboardResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppUser;
import com.cms.repository.AppUserRepository;
import com.cms.service.DashboardService;

/**
 * REST controller serving the aggregated dashboard summary and trend data.
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AppUserRepository appUserRepository;

    public DashboardController(DashboardService dashboardService,
                               AppUserRepository appUserRepository) {
        this.dashboardService = dashboardService;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Returns the ordered widget key list configured for the current user's role.
     * An empty list means "no config yet" — the frontend falls back to showing all widgets.
     */
    @GetMapping("/widgets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getUserDashboardWidgets(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        AppUser user = appUserRepository.findByKeycloakUsernameWithRole(username)
            .orElseThrow(() -> new ResourceNotFoundException("No user record for: " + username));
        List<String> widgets = (user.getAppRole() != null)
            ? user.getAppRole().getDashboardWidgets()
            : List.of();
        return ResponseEntity.ok(widgets);
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/trends")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardTrendsResponse> getTrends() {
        return ResponseEntity.ok(dashboardService.getTrends());
    }

    @GetMapping("/front-office")
    @PreAuthorize("@perm.has('ENQUIRY_VIEW')")
    public ResponseEntity<FrontOfficeDashboardResponse> getFrontOfficeDashboard() {
        return ResponseEntity.ok(dashboardService.getFrontOfficeDashboard());
    }

    /** Lightweight count of records currently eligible for collection — used by the paginator. */
    @GetMapping("/fee-collection-count")
    @PreAuthorize("@perm.has('FEE_COLLECT')")
    public ResponseEntity<Long> getFeeCollectionCount() {
        return ResponseEntity.ok(dashboardService.getFeeCollectionEligibleCount());
    }
}

