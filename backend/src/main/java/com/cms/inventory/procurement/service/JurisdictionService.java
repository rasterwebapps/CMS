package com.cms.inventory.procurement.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.procurement.model.InventoryTaxJurisdictionSetting;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.model.enums.JurisdictionMode;
import com.cms.inventory.procurement.repository.InventoryTaxJurisdictionSettingRepository;

/**
 * Resolves whether a purchase from a given {@link Supplier} is {@code INTERSTATE} or {@code
 * INTRASTATE}, by comparing the supplier's state against the institution's own home state (a
 * case-insensitive exact match — both fields are free-text state names, not coded values, so this
 * is the same comparison approach GST filing tools use for a state-name field). Called from
 * {@code PurchaseOrderService.addLine} whenever a line has a tax selected. See the "GAP-02
 * pickup" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class JurisdictionService {

    private final InventoryTaxJurisdictionSettingRepository settingRepository;

    public JurisdictionService(InventoryTaxJurisdictionSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /** Throws if the institution's home state hasn't been configured yet — deliberately blocking
     *  rather than guessing, per the QA round's "block with a clear validation error" decision. */
    public JurisdictionMode resolve(Supplier supplier) {
        InventoryTaxJurisdictionSetting setting = settingRepository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)
            .orElseThrow(() -> new IllegalStateException(
                "The institution's home state hasn't been configured yet — set it under Tax Jurisdiction Settings before ordering with tax"));

        String homeState = setting.getHomeState().trim();
        String supplierState = supplier.getState() == null ? "" : supplier.getState().trim();
        if (supplierState.isEmpty()) {
            throw new IllegalStateException(
                "Supplier '" + supplier.getSupplierName() + "' has no state on file — add one before ordering with tax");
        }
        return homeState.equalsIgnoreCase(supplierState) ? JurisdictionMode.INTRASTATE : JurisdictionMode.INTERSTATE;
    }
}
