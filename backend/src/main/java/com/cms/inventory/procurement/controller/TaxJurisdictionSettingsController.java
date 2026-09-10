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

import com.cms.inventory.procurement.dto.TaxJurisdictionSettingsRequest;
import com.cms.inventory.procurement.dto.TaxJurisdictionSettingsResponse;
import com.cms.inventory.procurement.service.TaxJurisdictionSettingsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/procurement/tax-jurisdiction-settings")
public class TaxJurisdictionSettingsController {

    private final TaxJurisdictionSettingsService settingsService;

    public TaxJurisdictionSettingsController(TaxJurisdictionSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_TAX_JURISDICTION_SETTINGS_VIEW', 'INVENTORY_TAX_JURISDICTION_SETTINGS_MANAGE')")
    public ResponseEntity<TaxJurisdictionSettingsResponse> find() {
        return ResponseEntity.ok(settingsService.find());
    }

    @PutMapping
    @PreAuthorize("@perm.has('INVENTORY_TAX_JURISDICTION_SETTINGS_MANAGE')")
    public ResponseEntity<TaxJurisdictionSettingsResponse> save(
            @Valid @RequestBody TaxJurisdictionSettingsRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(settingsService.save(request, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
