package com.cms.inventory.procurement.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.procurement.dto.PurchaseRequisitionResponse;
import com.cms.inventory.procurement.dto.WantedListConvertRequest;
import com.cms.inventory.procurement.dto.WantedListItemResponse;
import com.cms.inventory.procurement.dto.WantedListRejectRequest;
import com.cms.inventory.procurement.dto.WantedListResolutionRequest;
import com.cms.inventory.procurement.service.WantedListService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/procurement/wanted-list")
public class WantedListController {

    private static final String VIEW_ANY =
        "@perm.hasAny('INVENTORY_WANTED_LIST_VIEW', 'INVENTORY_WANTED_LIST_MANAGE', 'INVENTORY_WANTED_LIST_CONVERT', 'INVENTORY_WANTED_LIST_RUN')";

    private final WantedListService wantedListService;

    public WantedListController(WantedListService wantedListService) {
        this.wantedListService = wantedListService;
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<WantedListItemResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 25, sort = "generatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(wantedListService.findPage(locationId, status, pageable));
    }

    @PostMapping("/run")
    @PreAuthorize("@perm.has('INVENTORY_WANTED_LIST_RUN')")
    public ResponseEntity<Map<String, Integer>> run() {
        return ResponseEntity.ok(Map.of("created", wantedListService.generate()));
    }

    @PostMapping("/{id}/defer")
    @PreAuthorize("@perm.has('INVENTORY_WANTED_LIST_MANAGE')")
    public ResponseEntity<WantedListItemResponse> defer(@PathVariable Long id,
            @Valid @RequestBody(required = false) WantedListResolutionRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(wantedListService.defer(id, request != null ? request : new WantedListResolutionRequest(null), username(jwt)));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("@perm.has('INVENTORY_WANTED_LIST_MANAGE')")
    public ResponseEntity<WantedListItemResponse> reopen(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(wantedListService.reopen(id, username(jwt)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@perm.has('INVENTORY_WANTED_LIST_MANAGE')")
    public ResponseEntity<WantedListItemResponse> reject(@PathVariable Long id,
            @Valid @RequestBody WantedListRejectRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(wantedListService.reject(id, request, username(jwt)));
    }

    @PostMapping("/convert")
    @PreAuthorize("@perm.has('INVENTORY_WANTED_LIST_CONVERT')")
    public ResponseEntity<PurchaseRequisitionResponse> convert(@Valid @RequestBody WantedListConvertRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(wantedListService.convert(request, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
