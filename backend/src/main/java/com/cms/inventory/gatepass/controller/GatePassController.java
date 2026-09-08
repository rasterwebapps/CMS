package com.cms.inventory.gatepass.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.gatepass.dto.GatePassCreateRequest;
import com.cms.inventory.gatepass.dto.GatePassRejectRequest;
import com.cms.inventory.gatepass.dto.GatePassResponse;
import com.cms.inventory.gatepass.dto.GatePassReturnRequest;
import com.cms.inventory.gatepass.service.GatePassService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/gate-passes")
public class GatePassController {

    private static final String VIEW_ANY =
        "@perm.hasAny('INVENTORY_GATE_PASS_VIEW', 'INVENTORY_GATE_PASS_MANAGE', 'INVENTORY_GATE_PASS_APPROVE', 'INVENTORY_GATE_PASS_VERIFY', 'INVENTORY_GATE_PASS_RETURN')";

    private final GatePassService gatePassService;

    public GatePassController(GatePassService gatePassService) {
        this.gatePassService = gatePassService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_GATE_PASS_MANAGE')")
    public ResponseEntity<GatePassResponse> create(@Valid @RequestBody GatePassCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gatePassService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<GatePassResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean overdueOnly,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(gatePassService.findPage(locationId, direction, status, overdueOnly, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<GatePassResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(gatePassService.findById(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@perm.has('INVENTORY_GATE_PASS_APPROVE')")
    public ResponseEntity<GatePassResponse> approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(gatePassService.approve(id, username(jwt)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@perm.has('INVENTORY_GATE_PASS_APPROVE')")
    public ResponseEntity<GatePassResponse> reject(@PathVariable Long id, @Valid @RequestBody GatePassRejectRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(gatePassService.reject(id, request, username(jwt)));
    }

    @PostMapping("/{id}/verify-gate")
    @PreAuthorize("@perm.has('INVENTORY_GATE_PASS_VERIFY')")
    public ResponseEntity<GatePassResponse> verifyGate(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(gatePassService.verifyGate(id, username(jwt)));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("@perm.has('INVENTORY_GATE_PASS_RETURN')")
    public ResponseEntity<GatePassResponse> markReturned(@PathVariable Long id, @Valid @RequestBody(required = false) GatePassReturnRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(gatePassService.markReturned(id, request, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
