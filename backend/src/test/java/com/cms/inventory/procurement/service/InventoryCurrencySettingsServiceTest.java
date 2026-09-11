package com.cms.inventory.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.inventory.procurement.dto.InventoryCurrencySettingsRequest;
import com.cms.inventory.procurement.dto.InventoryCurrencySettingsResponse;
import com.cms.inventory.procurement.model.InventoryCurrencySetting;
import com.cms.inventory.procurement.repository.InventoryCurrencySettingRepository;

/** Covers the "Multi-currency FX" Phase 3 item's base-currency singleton — mirrors
 *  TaxJurisdictionSettingsService's own behavior exactly. */
@ExtendWith(MockitoExtension.class)
class InventoryCurrencySettingsServiceTest {

    @Mock private InventoryCurrencySettingRepository repository;

    private InventoryCurrencySettingsService service;

    @BeforeEach
    void setUp() {
        service = new InventoryCurrencySettingsService(repository);
    }

    @Test
    void findReturnsNullFieldsWhenNotConfigured() {
        when(repository.findById(InventoryCurrencySetting.SINGLETON_ID)).thenReturn(Optional.empty());

        InventoryCurrencySettingsResponse response = service.find();

        assertThat(response.baseCurrencyCode()).isNull();
    }

    @Test
    void findReturnsConfiguredBaseCurrency() {
        InventoryCurrencySetting setting = new InventoryCurrencySetting();
        setting.setBaseCurrencyCode("INR");
        when(repository.findById(InventoryCurrencySetting.SINGLETON_ID)).thenReturn(Optional.of(setting));

        assertThat(service.find().baseCurrencyCode()).isEqualTo("INR");
    }

    @Test
    void saveCreatesSingletonWhenNoneExistsAndUppercasesCode() {
        when(repository.findById(InventoryCurrencySetting.SINGLETON_ID)).thenReturn(Optional.empty());
        lenient().when(repository.save(any(InventoryCurrencySetting.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryCurrencySettingsResponse response = service.save(new InventoryCurrencySettingsRequest("inr"), "admin");

        assertThat(response.baseCurrencyCode()).isEqualTo("INR");
        assertThat(response.updatedBy()).isEqualTo("admin");
    }

    @Test
    void saveRejectsBlankCode() {
        assertThatThrownBy(() -> service.save(new InventoryCurrencySettingsRequest("   "), "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("required");
    }
}
