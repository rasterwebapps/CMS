package com.cms.inventory.procurement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.procurement.dto.InventoryCurrencySettingsRequest;
import com.cms.inventory.procurement.dto.InventoryCurrencySettingsResponse;
import com.cms.inventory.procurement.service.InventoryCurrencySettingsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/procurement/currency-settings")
public class InventoryCurrencySettingsController {

    private final InventoryCurrencySettingsService settingsService;

    public InventoryCurrencySettingsController(InventoryCurrencySettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_CURRENCY_SETTINGS_VIEW', 'INVENTORY_CURRENCY_SETTINGS_MANAGE')")
    public ResponseEntity<InventoryCurrencySettingsResponse> find() {
        return ResponseEntity.ok(settingsService.find());
    }

    @PutMapping
    @PreAuthorize("@perm.has('INVENTORY_CURRENCY_SETTINGS_MANAGE')")
    public ResponseEntity<InventoryCurrencySettingsResponse> save(
            @Valid @RequestBody InventoryCurrencySettingsRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(settingsService.save(request, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
