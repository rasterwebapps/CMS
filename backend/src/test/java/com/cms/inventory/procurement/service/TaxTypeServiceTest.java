package com.cms.inventory.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.procurement.dto.TaxTypeRequest;
import com.cms.inventory.procurement.model.TaxType;
import com.cms.inventory.procurement.repository.TaxTypeRepository;

@ExtendWith(MockitoExtension.class)
class TaxTypeServiceTest {

    @Mock private TaxTypeRepository repository;
    private TaxTypeService service;

    @BeforeEach
    void setUp() {
        service = new TaxTypeService(repository);
    }

    @Test
    void shouldCreateTaxType() {
        when(repository.existsByNameIgnoreCase("GST")).thenReturn(false);
        when(repository.save(any(TaxType.class))).thenAnswer(inv -> { TaxType t = inv.getArgument(0); t.setId(1L); return t; });
        var res = service.create(new TaxTypeRequest("GST", "Goods and Services Tax", true));
        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.name()).isEqualTo("GST");
    }

    @Test
    void shouldRejectDuplicateNameOnCreate() {
        when(repository.existsByNameIgnoreCase("GST")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new TaxTypeRequest("GST", null, true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldRejectBlankNameOnCreate() {
        assertThatThrownBy(() -> service.create(new TaxTypeRequest("   ", null, true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Name is required");
    }

    @Test
    void shouldListOnlyActiveWhenRequested() {
        when(repository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(type(1L, "GST")));
        assertThat(service.findAll(true)).singleElement().satisfies(r -> assertThat(r.name()).isEqualTo("GST"));
    }

    @Test
    void shouldUpdateTaxType() {
        TaxType existing = type(1L, "OLD");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameIgnoreCaseAndIdNot("VAT", 1L)).thenReturn(false);
        when(repository.save(any(TaxType.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.update(1L, new TaxTypeRequest("VAT", null, true)).name()).isEqualTo("VAT");
    }

    @Test
    void shouldRejectDuplicateNameOnUpdate() {
        when(repository.findById(1L)).thenReturn(Optional.of(type(1L, "OLD")));
        when(repository.existsByNameIgnoreCaseAndIdNot("VAT", 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.update(1L, new TaxTypeRequest("VAT", null, true)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDeleteExistingTaxType() {
        when(repository.existsById(1L)).thenReturn(true);
        service.delete(1L);
    }

    @Test
    void shouldThrowWhenDeletingMissingTaxType() {
        when(repository.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateStatus() {
        TaxType existing = type(1L, "GST");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(TaxType.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.updateStatus(1L, new ActiveStatusUpdateRequest(false, null)).isActive()).isFalse();
    }

    @Test
    void shouldThrowWhenNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldCheckNameExistsExcludingSelf() {
        when(repository.existsByNameIgnoreCaseAndIdNot("GST", 1L)).thenReturn(false);
        assertThat(service.nameExists("GST", 1L)).isFalse();
    }

    private TaxType type(Long id, String name) {
        TaxType t = new TaxType();
        t.setId(id);
        t.setName(name);
        t.setIsActive(true);
        return t;
    }
}
