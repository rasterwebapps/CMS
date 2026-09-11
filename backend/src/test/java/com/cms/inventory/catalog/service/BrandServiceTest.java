package com.cms.inventory.catalog.service;

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

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.BrandRequest;
import com.cms.inventory.catalog.dto.BrandResponse;
import com.cms.inventory.catalog.model.Brand;
import com.cms.inventory.catalog.repository.BrandRepository;

/** Covers the "Brand/Manufacturer master" Phase 1 item — same shape as UomService minus code. */
@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock private BrandRepository brandRepository;

    private BrandService service;

    @BeforeEach
    void setUp() {
        service = new BrandService(brandRepository);
        lenient().when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> {
            Brand b = inv.getArgument(0);
            if (b.getId() == null) b.setId(1L);
            return b;
        });
    }

    @Test
    void createsBrandWithTrimmedNameAndDescription() {
        when(brandRepository.existsByNameIgnoreCase("Dell")).thenReturn(false);

        BrandResponse response = service.create(new BrandRequest("  Dell  ", "  Computers  ", null));

        assertThat(response.name()).isEqualTo("Dell");
        assertThat(response.description()).isEqualTo("Computers");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> service.create(new BrandRequest("   ", null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("required");
    }

    @Test
    void rejectsDuplicateNameOnCreate() {
        when(brandRepository.existsByNameIgnoreCase("Dell")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new BrandRequest("Dell", null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void updatesBrandExcludingItselfFromDuplicateCheck() {
        Brand existing = new Brand();
        existing.setId(5L);
        existing.setName("Dell");
        when(brandRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(brandRepository.existsByNameIgnoreCaseAndIdNot("Dell Inc", 5L)).thenReturn(false);

        BrandResponse response = service.update(5L, new BrandRequest("Dell Inc", null, null));

        assertThat(response.name()).isEqualTo("Dell Inc");
    }

    @Test
    void rejectsDuplicateNameOnUpdate() {
        Brand existing = new Brand();
        existing.setId(5L);
        existing.setName("Dell");
        when(brandRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(brandRepository.existsByNameIgnoreCaseAndIdNot("HP", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, new BrandRequest("HP", null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void throwsWhenUpdatingUnknownBrand() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new BrandRequest("Dell", null, null)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void togglesActiveStatus() {
        Brand existing = new Brand();
        existing.setId(5L);
        existing.setName("Dell");
        existing.setIsActive(true);
        when(brandRepository.findById(5L)).thenReturn(Optional.of(existing));

        ActiveStatusUpdateResponse response = service.updateStatus(5L, new ActiveStatusUpdateRequest(false, null));

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void deleteThrowsWhenBrandNotFound() {
        when(brandRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
