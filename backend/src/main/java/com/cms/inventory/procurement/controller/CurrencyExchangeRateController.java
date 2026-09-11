package com.cms.inventory.procurement.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.inventory.procurement.dto.CurrencyExchangeRateRequest;
import com.cms.inventory.procurement.dto.CurrencyExchangeRateResponse;
import com.cms.inventory.procurement.service.CurrencyExchangeRateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/procurement/currency-exchange-rates")
public class CurrencyExchangeRateController {

    private final CurrencyExchangeRateService rateService;

    public CurrencyExchangeRateController(CurrencyExchangeRateService rateService) {
        this.rateService = rateService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_CURRENCY_EXCHANGE_RATE_MANAGE')")
    public ResponseEntity<CurrencyExchangeRateResponse> create(@Valid @RequestBody CurrencyExchangeRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rateService.create(request));
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('INVENTORY_CURRENCY_EXCHANGE_RATE_VIEW', 'INVENTORY_CURRENCY_EXCHANGE_RATE_MANAGE')")
    public ResponseEntity<List<CurrencyExchangeRateResponse>> findAll() {
        return ResponseEntity.ok(rateService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_CURRENCY_EXCHANGE_RATE_VIEW', 'INVENTORY_CURRENCY_EXCHANGE_RATE_MANAGE')")
    public ResponseEntity<CurrencyExchangeRateResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(rateService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_CURRENCY_EXCHANGE_RATE_MANAGE')")
    public ResponseEntity<CurrencyExchangeRateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CurrencyExchangeRateRequest request) {
        return ResponseEntity.ok(rateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_CURRENCY_EXCHANGE_RATE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_CURRENCY_EXCHANGE_RATE_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(rateService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_CURRENCY_EXCHANGE_RATE_VIEW', 'INVENTORY_CURRENCY_EXCHANGE_RATE_MANAGE')")
    public ResponseEntity<Page<CurrencyExchangeRateResponse>> findPage(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "currencyCode", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(rateService.findPage(search, pageable));
    }

    // value = effectiveDate (the field checked as the user picks it), currencyCode = scope —
    // matches the shared uniqueFieldValidator convention (see VendorProductMapping's pair-exists).
    @GetMapping("/pair-exists")
    @PreAuthorize("@perm.has('INVENTORY_CURRENCY_EXCHANGE_RATE_MANAGE')")
    public ResponseEntity<Boolean> pairExists(
            @RequestParam LocalDate value, @RequestParam String currencyCode, @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(rateService.pairExists(currencyCode, value, excludeId));
    }
}
