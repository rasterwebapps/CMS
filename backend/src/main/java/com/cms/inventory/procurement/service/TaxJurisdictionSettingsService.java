package com.cms.inventory.procurement.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.procurement.dto.TaxJurisdictionSettingsRequest;
import com.cms.inventory.procurement.dto.TaxJurisdictionSettingsResponse;
import com.cms.inventory.procurement.model.InventoryTaxJurisdictionSetting;
import com.cms.inventory.procurement.repository.InventoryTaxJurisdictionSettingRepository;

/**
 * The institution's own home state (a single-row singleton, {@link InventoryTaxJurisdictionSetting#SINGLETON_ID})
 * — compared against a supplier's state to resolve {@code JurisdictionMode} on each PO line. See
 * {@code JurisdictionService} and the "GAP-02 pickup" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class TaxJurisdictionSettingsService {

    private final InventoryTaxJurisdictionSettingRepository repository;

    public TaxJurisdictionSettingsService(InventoryTaxJurisdictionSettingRepository repository) {
        this.repository = repository;
    }

    /** Empty when jurisdiction hasn't been configured yet — callers decide how to react. */
    public TaxJurisdictionSettingsResponse find() {
        return repository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)
            .map(s -> new TaxJurisdictionSettingsResponse(s.getHomeState(), s.getUpdatedAt(), s.getUpdatedBy()))
            .orElse(new TaxJurisdictionSettingsResponse(null, null, null));
    }

    @Transactional
    public TaxJurisdictionSettingsResponse save(TaxJurisdictionSettingsRequest request, String actor) {
        String homeState = requireTrimmed(request.homeState(), "Home state is required");
        InventoryTaxJurisdictionSetting setting = repository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)
            .orElseGet(InventoryTaxJurisdictionSetting::new);
        setting.setHomeState(homeState);
        setting.setUpdatedAt(Instant.now());
        setting.setUpdatedBy(actor);
        InventoryTaxJurisdictionSetting saved = repository.save(setting);
        return new TaxJurisdictionSettingsResponse(saved.getHomeState(), saved.getUpdatedAt(), saved.getUpdatedBy());
    }

    private static String requireTrimmed(String s, String message) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(message);
        return t;
    }
}
