package com.cms.inventory.indent.controller;

import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.indent.dto.StockIndentAddLineRequest;
import com.cms.inventory.indent.dto.StockIndentCreateRequest;
import com.cms.inventory.indent.dto.StockIndentFulfillViaTransferRequest;
import com.cms.inventory.indent.dto.StockIndentFulfillmentContextResponse;
import com.cms.inventory.indent.dto.StockIndentItemResponse;
import com.cms.inventory.indent.dto.StockIndentRaisePoRequest;
import com.cms.inventory.indent.dto.StockIndentResolutionRequest;
import com.cms.inventory.indent.dto.StockIndentResponse;
import com.cms.inventory.indent.dto.StockIndentReturnLineRequest;
import com.cms.inventory.indent.service.AutoIndentService;
import com.cms.inventory.indent.service.StockIndentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/indent/stock-indents")
public class StockIndentController {

    private static final String VIEW_ANY =
        "@perm.hasAny('INVENTORY_STOCK_INDENT_VIEW', 'INVENTORY_STOCK_INDENT_MANAGE', 'INVENTORY_STOCK_INDENT_APPROVE')";

    private final StockIndentService requestService;
    private final AutoIndentService autoIndentService;

    public StockIndentController(StockIndentService requestService, AutoIndentService autoIndentService) {
        this.requestService = requestService;
        this.autoIndentService = autoIndentService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_MANAGE')")
    public ResponseEntity<StockIndentResponse> create(@Valid @RequestBody StockIndentCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<StockIndentResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(requestService.findPage(locationId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<StockIndentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.findById(id));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_MANAGE')")
    public ResponseEntity<StockIndentItemResponse> addLine(@PathVariable Long id, @Valid @RequestBody StockIndentAddLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.addLine(id, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_MANAGE')")
    public ResponseEntity<Void> removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        requestService.removeLine(id, lineId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_MANAGE')")
    public ResponseEntity<StockIndentResponse> submit(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.submit(id, username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/approve")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_APPROVE')")
    public ResponseEntity<StockIndentItemResponse> approveLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody(required = false) StockIndentResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.approveLine(id, lineId, request != null ? request : new StockIndentResolutionRequest(null), username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/reject")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_APPROVE')")
    public ResponseEntity<StockIndentItemResponse> rejectLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody(required = false) StockIndentResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.rejectLine(id, lineId, request != null ? request : new StockIndentResolutionRequest(null), username(jwt)));
    }

    @GetMapping("/{id}/lines/{lineId}/fulfillment-context")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<StockIndentFulfillmentContextResponse> getFulfillmentContext(@PathVariable Long id, @PathVariable Long lineId) {
        return ResponseEntity.ok(requestService.getFulfillmentContext(id, lineId));
    }

    @PostMapping("/{id}/lines/{lineId}/fulfill")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_FULFILL')")
    public ResponseEntity<StockIndentItemResponse> fulfillLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody(required = false) StockIndentResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.fulfillLine(id, lineId, request != null ? request : new StockIndentResolutionRequest(null), username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/fulfill-via-transfer")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_FULFILL')")
    public ResponseEntity<StockIndentItemResponse> fulfillViaTransferLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody StockIndentFulfillViaTransferRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.fulfillViaTransferLine(id, lineId, request, username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/raise-po")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_FULFILL')")
    public ResponseEntity<StockIndentItemResponse> raisePoLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody StockIndentRaisePoRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.raisePoLine(id, lineId, request, username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/deny")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_FULFILL')")
    public ResponseEntity<StockIndentItemResponse> denyLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody(required = false) StockIndentResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.denyLine(id, lineId, request != null ? request : new StockIndentResolutionRequest(null), username(jwt)));
    }

    @PostMapping("/{id}/lines/{lineId}/return")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_RETURN')")
    public ResponseEntity<StockIndentItemResponse> returnLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody StockIndentReturnLineRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(requestService.returnLine(id, lineId, request, username(jwt)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_MANAGE')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        requestService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auto-run")
    @PreAuthorize("@perm.has('INVENTORY_STOCK_INDENT_AUTO_RUN')")
    public ResponseEntity<Map<String, Integer>> autoRun() {
        return ResponseEntity.ok(Map.of("created", autoIndentService.generate()));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
