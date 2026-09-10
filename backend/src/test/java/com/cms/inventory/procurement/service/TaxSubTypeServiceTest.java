package com.cms.inventory.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.procurement.dto.TaxSubTypeRequest;
import com.cms.inventory.procurement.model.TaxRule;
import com.cms.inventory.procurement.model.TaxSubType;
import com.cms.inventory.procurement.model.enums.JurisdictionMode;
import com.cms.inventory.procurement.repository.TaxRuleRepository;
import com.cms.inventory.procurement.repository.TaxSubTypeRepository;

@ExtendWith(MockitoExtension.class)
class TaxSubTypeServiceTest {

    @Mock private TaxSubTypeRepository taxSubTypeRepository;
    @Mock private TaxRuleRepository taxRuleRepository;
    private TaxSubTypeService service;

    @BeforeEach
    void setUp() {
        service = new TaxSubTypeService(taxSubTypeRepository, taxRuleRepository);
    }

    @Test
    void shouldCreateSubTypeWithinCap() {
        when(taxRuleRepository.findById(1L)).thenReturn(Optional.of(taxRule(1L, "GST 18%")));
        when(taxSubTypeRepository.existsByTaxRule_IdAndJurisdictionModeAndComponentNameIgnoreCase(1L, JurisdictionMode.INTRASTATE, "CGST"))
            .thenReturn(false);
        when(taxSubTypeRepository.findByTaxRule_IdAndJurisdictionMode(1L, JurisdictionMode.INTRASTATE)).thenReturn(List.of());
        when(taxSubTypeRepository.save(any(TaxSubType.class))).thenAnswer(inv -> { TaxSubType s = inv.getArgument(0); s.setId(5L); return s; });

        var res = service.create(new TaxSubTypeRequest(1L, "INTRASTATE", "CGST", new BigDecimal("50"), true));
        assertThat(res.id()).isEqualTo(5L);
        assertThat(res.componentName()).isEqualTo("CGST");
        assertThat(res.jurisdictionMode()).isEqualTo("INTRASTATE");
    }

    @Test
    void shouldRejectDuplicateComponentNameOnCreate() {
        when(taxRuleRepository.findById(1L)).thenReturn(Optional.of(taxRule(1L, "GST 18%")));
        when(taxSubTypeRepository.existsByTaxRule_IdAndJurisdictionModeAndComponentNameIgnoreCase(1L, JurisdictionMode.INTRASTATE, "CGST"))
            .thenReturn(true);
        assertThatThrownBy(() -> service.create(new TaxSubTypeRequest(1L, "INTRASTATE", "CGST", new BigDecimal("50"), true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldRejectWhenNewSplitPushesModeOver100() {
        when(taxRuleRepository.findById(1L)).thenReturn(Optional.of(taxRule(1L, "GST 18%")));
        when(taxSubTypeRepository.existsByTaxRule_IdAndJurisdictionModeAndComponentNameIgnoreCase(1L, JurisdictionMode.INTRASTATE, "SGST"))
            .thenReturn(false);
        when(taxSubTypeRepository.findByTaxRule_IdAndJurisdictionMode(1L, JurisdictionMode.INTRASTATE))
            .thenReturn(List.of(subType(1L, JurisdictionMode.INTRASTATE, "CGST", new BigDecimal("60"))));
        assertThatThrownBy(() -> service.create(new TaxSubTypeRequest(1L, "INTRASTATE", "SGST", new BigDecimal("50"), true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot exceed 100%");
    }

    @Test
    void shouldRejectInvalidJurisdictionMode() {
        when(taxRuleRepository.findById(1L)).thenReturn(Optional.of(taxRule(1L, "GST 18%")));
        assertThatThrownBy(() -> service.create(new TaxSubTypeRequest(1L, "GLOBAL", "CGST", new BigDecimal("50"), true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid jurisdiction mode");
    }

    @Test
    void shouldThrowWhenTaxRuleNotFoundOnCreate() {
        when(taxRuleRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(new TaxSubTypeRequest(9L, "INTRASTATE", "CGST", new BigDecimal("50"), true)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldExcludeSelfWhenCheckingCapOnUpdate() {
        TaxSubType existing = subType(3L, JurisdictionMode.INTRASTATE, "CGST", new BigDecimal("50"));
        when(taxSubTypeRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(taxRuleRepository.findById(1L)).thenReturn(Optional.of(taxRule(1L, "GST 18%")));
        when(taxSubTypeRepository.existsByTaxRule_IdAndJurisdictionModeAndComponentNameIgnoreCaseAndIdNot(1L, JurisdictionMode.INTRASTATE, "CGST", 3L))
            .thenReturn(false);
        // Only this row (id=3) already exists at 50% — excluding itself, raising it to 90% should pass.
        when(taxSubTypeRepository.findByTaxRule_IdAndJurisdictionMode(1L, JurisdictionMode.INTRASTATE))
            .thenReturn(List.of(existing));
        when(taxSubTypeRepository.save(any(TaxSubType.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = service.update(3L, new TaxSubTypeRequest(1L, "INTRASTATE", "CGST", new BigDecimal("90"), true));
        assertThat(res.splitPercent()).isEqualByComparingTo("90");
    }

    @Test
    void shouldDeleteExistingSubType() {
        when(taxSubTypeRepository.existsById(3L)).thenReturn(true);
        service.delete(3L);
    }

    @Test
    void shouldThrowWhenDeletingMissingSubType() {
        when(taxSubTypeRepository.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnCompleteSplitWhenSumsToExactly100() {
        when(taxSubTypeRepository.findByTaxRule_IdAndJurisdictionModeAndIsActiveTrueOrderByComponentNameAsc(1L, JurisdictionMode.INTRASTATE))
            .thenReturn(List.of(
                subType(1L, JurisdictionMode.INTRASTATE, "CGST", new BigDecimal("50")),
                subType(2L, JurisdictionMode.INTRASTATE, "SGST", new BigDecimal("50"))));
        assertThat(service.requireCompleteSplit(1L, JurisdictionMode.INTRASTATE)).hasSize(2);
    }

    @Test
    void shouldThrowWhenNoComponentsConfigured() {
        when(taxSubTypeRepository.findByTaxRule_IdAndJurisdictionModeAndIsActiveTrueOrderByComponentNameAsc(1L, JurisdictionMode.INTERSTATE))
            .thenReturn(List.of());
        assertThatThrownBy(() -> service.requireCompleteSplit(1L, JurisdictionMode.INTERSTATE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no interstate tax components configured");
    }

    @Test
    void shouldThrowWhenComponentsDoNotSumToExactly100() {
        when(taxSubTypeRepository.findByTaxRule_IdAndJurisdictionModeAndIsActiveTrueOrderByComponentNameAsc(1L, JurisdictionMode.INTRASTATE))
            .thenReturn(List.of(subType(1L, JurisdictionMode.INTRASTATE, "CGST", new BigDecimal("40"))));
        assertThatThrownBy(() -> service.requireCompleteSplit(1L, JurisdictionMode.INTRASTATE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not 100%");
    }

    private TaxRule taxRule(Long id, String name) {
        TaxRule t = new TaxRule();
        t.setId(id);
        t.setName(name);
        t.setRatePercent(new BigDecimal("18"));
        return t;
    }

    private TaxSubType subType(Long id, JurisdictionMode mode, String name, BigDecimal percent) {
        TaxSubType s = new TaxSubType();
        s.setId(id);
        s.setTaxRule(taxRule(1L, "GST 18%"));
        s.setJurisdictionMode(mode);
        s.setComponentName(name);
        s.setSplitPercent(percent);
        s.setIsActive(true);
        return s;
    }
}
