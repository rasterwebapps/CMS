package com.cms.inventory.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.inventory.procurement.dto.TaxJurisdictionSettingsRequest;
import com.cms.inventory.procurement.model.InventoryTaxJurisdictionSetting;
import com.cms.inventory.procurement.repository.InventoryTaxJurisdictionSettingRepository;

@ExtendWith(MockitoExtension.class)
class TaxJurisdictionSettingsServiceTest {

    @Mock private InventoryTaxJurisdictionSettingRepository repository;
    private TaxJurisdictionSettingsService service;

    @BeforeEach
    void setUp() {
        service = new TaxJurisdictionSettingsService(repository);
    }

    @Test
    void shouldReturnEmptyResponseWhenNotConfigured() {
        when(repository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)).thenReturn(Optional.empty());
        var res = service.find();
        assertThat(res.homeState()).isNull();
    }

    @Test
    void shouldReturnConfiguredHomeState() {
        InventoryTaxJurisdictionSetting existing = new InventoryTaxJurisdictionSetting();
        existing.setHomeState("Tamil Nadu");
        when(repository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)).thenReturn(Optional.of(existing));
        assertThat(service.find().homeState()).isEqualTo("Tamil Nadu");
    }

    @Test
    void shouldCreateSettingWhenNoneExistsYet() {
        when(repository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)).thenReturn(Optional.empty());
        when(repository.save(any(InventoryTaxJurisdictionSetting.class))).thenAnswer(inv -> inv.getArgument(0));
        var res = service.save(new TaxJurisdictionSettingsRequest("Tamil Nadu"), "admin");
        assertThat(res.homeState()).isEqualTo("Tamil Nadu");
        assertThat(res.updatedBy()).isEqualTo("admin");
    }

    @Test
    void shouldOverwriteExistingSetting() {
        InventoryTaxJurisdictionSetting existing = new InventoryTaxJurisdictionSetting();
        existing.setHomeState("Kerala");
        when(repository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(InventoryTaxJurisdictionSetting.class))).thenAnswer(inv -> inv.getArgument(0));
        var res = service.save(new TaxJurisdictionSettingsRequest("Tamil Nadu"), "admin");
        assertThat(res.homeState()).isEqualTo("Tamil Nadu");
    }

    @Test
    void shouldRejectBlankHomeState() {
        assertThatThrownBy(() -> service.save(new TaxJurisdictionSettingsRequest("  "), "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Home state is required");
    }
}
