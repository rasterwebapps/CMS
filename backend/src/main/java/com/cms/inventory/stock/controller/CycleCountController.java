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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.stock.dto.CycleCountAddLineRequest;
import com.cms.inventory.stock.dto.CycleCountCreateRequest;
import com.cms.inventory.stock.dto.CycleCountEnterCountRequest;
import com.cms.inventory.stock.dto.CycleCountLineResponse;
import com.cms.inventory.stock.dto.CycleCountResolutionRequest;
import com.cms.inventory.stock.dto.CycleCountResponse;
import com.cms.inventory.stock.service.CycleCountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/stock/cycle-counts")
public class CycleCountController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_CYCLE_COUNT_VIEW', 'INVENTORY_CYCLE_COUNT_MANAGE', 'INVENTORY_CYCLE_COUNT_APPROVE')";

    private final CycleCountService cycleCountService;

    public CycleCountController(CycleCountService cycleCountService) {
        this.cycleCountService = cycleCountService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_CYCLE_COUNT_MANAGE')")
    public ResponseEntity<CycleCountResponse> create(@Valid @RequestBody CycleCountCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cycleCountService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<CycleCountResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cycleCountService.findPage(locationId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<CycleCountResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(cycleCountService.findById(id));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("@perm.has('INVENTORY_CYCLE_COUNT_MANAGE')")
    public ResponseEntity<CycleCountLineResponse> addLine(@PathVariable Long id, @Valid @RequestBody CycleCountAddLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cycleCountService.addLine(id, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("@perm.has('INVENTORY_CYCLE_COUNT_MANAGE')")
    public ResponseEntity<Void> removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        cycleCountService.removeLine(id, lineId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/lines/{lineId}/count")
    @PreAuthorize("@perm.has('INVENTORY_CYCLE_COUNT_MANAGE')")
    public ResponseEntity<CycleCountLineResponse> enterCount(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody CycleCountEnterCountRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cycleCountService.enterCount(id, lineId, request, username(jwt)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@perm.has('INVENTORY_CYCLE_COUNT_MANAGE')")
    public ResponseEntity<CycleCountResponse> submit(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cycleCountService.submit(id, username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/approve")
    @PreAuthorize("@perm.has('INVENTORY_CYCLE_COUNT_APPROVE')")
    public ResponseEntity<CycleCountLineResponse> approveLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody(required = false) CycleCountResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cycleCountService.approveLine(id, lineId, request != null ? request : new CycleCountResolutionRequest(null), username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/reject")
    @PreAuthorize("@perm.has('INVENTORY_CYCLE_COUNT_APPROVE')")
    public ResponseEntity<CycleCountLineResponse> rejectLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody(required = false) CycleCountResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cycleCountService.rejectLine(id, lineId, request != null ? request : new CycleCountResolutionRequest(null), username(jwt)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('INVENTORY_CYCLE_COUNT_MANAGE')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        cycleCountService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
