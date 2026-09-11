package com.cms.inventory.procurement.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.procurement.dto.InventoryCurrencySettingsRequest;
import com.cms.inventory.procurement.dto.InventoryCurrencySettingsResponse;
import com.cms.inventory.procurement.model.InventoryCurrencySetting;
import com.cms.inventory.procurement.repository.InventoryCurrencySettingRepository;

/**
 * The institution's own base currency (a single-row singleton, {@link
 * InventoryCurrencySetting#SINGLETON_ID}) — every {@code CurrencyExchangeRate} converts to this
 * currency. See the 2026-09-11 "Multi-currency FX" decision-log entry; mirrors {@code
 * TaxJurisdictionSettingsService} exactly.
 */
@Service
@Transactional(readOnly = true)
public class InventoryCurrencySettingsService {

    private final InventoryCurrencySettingRepository repository;

    public InventoryCurrencySettingsService(InventoryCurrencySettingRepository repository) {
        this.repository = repository;
    }

    /** Empty when the base currency hasn't been configured yet — callers decide how to react. */
    public InventoryCurrencySettingsResponse find() {
        return repository.findById(InventoryCurrencySetting.SINGLETON_ID)
            .map(s -> new InventoryCurrencySettingsResponse(s.getBaseCurrencyCode(), s.getUpdatedAt(), s.getUpdatedBy()))
            .orElse(new InventoryCurrencySettingsResponse(null, null, null));
    }

    /** Package-visible: the plain currency code, or {@code null} if unconfigured — used by {@code
     *  VendorProductMappingService} without forcing every caller through the full response DTO. */
    String findBaseCurrencyCodeOrNull() {
        return repository.findById(InventoryCurrencySetting.SINGLETON_ID)
            .map(InventoryCurrencySetting::getBaseCurrencyCode)
            .orElse(null);
    }

    @Transactional
    public InventoryCurrencySettingsResponse save(InventoryCurrencySettingsRequest request, String actor) {
        String code = requireTrimmed(request.baseCurrencyCode(), "Base currency code is required").toUpperCase();
        InventoryCurrencySetting setting = repository.findById(InventoryCurrencySetting.SINGLETON_ID)
            .orElseGet(InventoryCurrencySetting::new);
        setting.setBaseCurrencyCode(code);
        setting.setUpdatedAt(Instant.now());
        setting.setUpdatedBy(actor);
        InventoryCurrencySetting saved = repository.save(setting);
        return new InventoryCurrencySettingsResponse(saved.getBaseCurrencyCode(), saved.getUpdatedAt(), saved.getUpdatedBy());
    }

    private static String requireTrimmed(String s, String message) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(message);
        return t;
    }
}
