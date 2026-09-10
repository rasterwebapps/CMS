package com.cms.inventory.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.inventory.procurement.model.InventoryTaxJurisdictionSetting;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.model.enums.JurisdictionMode;
import com.cms.inventory.procurement.repository.InventoryTaxJurisdictionSettingRepository;

@ExtendWith(MockitoExtension.class)
class JurisdictionServiceTest {

    @Mock private InventoryTaxJurisdictionSettingRepository settingRepository;
    private JurisdictionService service;

    @BeforeEach
    void setUp() {
        service = new JurisdictionService(settingRepository);
    }

    @Test
    void shouldResolveIntrastateWhenSupplierStateMatchesHomeState() {
        when(settingRepository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)).thenReturn(Optional.of(setting("Tamil Nadu")));
        assertThat(service.resolve(supplier("Tamil Nadu"))).isEqualTo(JurisdictionMode.INTRASTATE);
    }

    @Test
    void shouldResolveIntrastateCaseInsensitively() {
        when(settingRepository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)).thenReturn(Optional.of(setting("Tamil Nadu")));
        assertThat(service.resolve(supplier("  tamil nadu  "))).isEqualTo(JurisdictionMode.INTRASTATE);
    }

    @Test
    void shouldResolveInterstateWhenSupplierStateDiffers() {
        when(settingRepository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)).thenReturn(Optional.of(setting("Tamil Nadu")));
        assertThat(service.resolve(supplier("Kerala"))).isEqualTo(JurisdictionMode.INTERSTATE);
    }

    @Test
    void shouldThrowWhenHomeStateNotConfigured() {
        when(settingRepository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolve(supplier("Kerala")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("home state hasn't been configured");
    }

    @Test
    void shouldThrowWhenSupplierHasNoState() {
        when(settingRepository.findById(InventoryTaxJurisdictionSetting.SINGLETON_ID)).thenReturn(Optional.of(setting("Tamil Nadu")));
        assertThatThrownBy(() -> service.resolve(supplier(null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("has no state on file");
    }

    private InventoryTaxJurisdictionSetting setting(String homeState) {
        InventoryTaxJurisdictionSetting s = new InventoryTaxJurisdictionSetting();
        s.setHomeState(homeState);
        return s;
    }

    private Supplier supplier(String state) {
        Supplier s = new Supplier();
        s.setId(1L);
        s.setSupplierName("Acme Supplies");
        s.setState(state);
        return s;
    }
}
