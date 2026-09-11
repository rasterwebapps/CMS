package com.cms.inventory.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
import com.cms.inventory.catalog.dto.UomConversionTemplateLevelRequest;
import com.cms.inventory.catalog.dto.UomConversionTemplateRequest;
import com.cms.inventory.catalog.dto.UomConversionTemplateResponse;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.model.UomConversionTemplate;
import com.cms.inventory.catalog.repository.UomConversionTemplateRepository;

/**
 * Covers the "Shared/global UOM conversion templates" Phase 2 item — level validation mirrors
 * {@code ProductUomChainServiceTest}'s coverage of the same rules on the product-chain side.
 */
@ExtendWith(MockitoExtension.class)
class UomConversionTemplateServiceTest {

    @Mock private UomConversionTemplateRepository templateRepository;
    @Mock private UomService uomService;

    private UomConversionTemplateService service;

    private Uom tablet;
    private Uom strip;

    @BeforeEach
    void setUp() {
        service = new UomConversionTemplateService(templateRepository, uomService);

        tablet = new Uom();
        tablet.setId(1L);
        tablet.setCode("TABLET");
        tablet.setName("Tablet");

        strip = new Uom();
        strip.setId(2L);
        strip.setCode("STRIP");
        strip.setName("Strip");

        lenient().when(uomService.findOrThrow(1L)).thenReturn(tablet);
        lenient().when(uomService.findOrThrow(2L)).thenReturn(strip);
        lenient().when(templateRepository.save(any(UomConversionTemplate.class))).thenAnswer(inv -> {
            UomConversionTemplate t = inv.getArgument(0);
            if (t.getId() == null) t.setId(100L);
            return t;
        });
    }

    private List<UomConversionTemplateLevelRequest> validLevels() {
        return List.of(
            new UomConversionTemplateLevelRequest(1L, 0, BigDecimal.ONE, false),
            new UomConversionTemplateLevelRequest(2L, 1, new BigDecimal("10"), true));
    }

    private UomConversionTemplateRequest request(List<UomConversionTemplateLevelRequest> levels) {
        return new UomConversionTemplateRequest("Pharma Tablet Pack", "Tablet -> Strip", 1L, null, levels);
    }

    @Test
    void createsTemplateWithLevels() {
        when(templateRepository.existsByNameIgnoreCase("Pharma Tablet Pack")).thenReturn(false);

        UomConversionTemplateResponse response = service.create(request(validLevels()));

        assertThat(response.name()).isEqualTo("Pharma Tablet Pack");
        assertThat(response.baseUomId()).isEqualTo(1L);
        assertThat(response.levels()).hasSize(2);
        assertThat(response.levels().get(1).factorToBase()).isEqualByComparingTo("10");
    }

    @Test
    void rejectsBlankName() {
        var req = new UomConversionTemplateRequest("   ", null, 1L, null, validLevels());

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("required");
    }

    @Test
    void rejectsDuplicateName() {
        when(templateRepository.existsByNameIgnoreCase("Pharma Tablet Pack")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(validLevels())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void rejectsLevelZeroNotMatchingBaseUom() {
        when(templateRepository.existsByNameIgnoreCase("Pharma Tablet Pack")).thenReturn(false);
        List<UomConversionTemplateLevelRequest> badLevels = List.of(
            new UomConversionTemplateLevelRequest(2L, 0, BigDecimal.ONE, false));

        assertThatThrownBy(() -> service.create(request(badLevels)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("base unit");
    }

    @Test
    void rejectsMissingLevelZero() {
        when(templateRepository.existsByNameIgnoreCase("Pharma Tablet Pack")).thenReturn(false);
        List<UomConversionTemplateLevelRequest> badLevels = List.of(
            new UomConversionTemplateLevelRequest(2L, 1, new BigDecimal("10"), false));

        assertThatThrownBy(() -> service.create(request(badLevels)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("level 0");
    }

    @Test
    void rejectsDuplicateRank() {
        when(templateRepository.existsByNameIgnoreCase("Pharma Tablet Pack")).thenReturn(false);
        List<UomConversionTemplateLevelRequest> badLevels = List.of(
            new UomConversionTemplateLevelRequest(1L, 0, BigDecimal.ONE, false),
            new UomConversionTemplateLevelRequest(2L, 0, new BigDecimal("10"), false));

        assertThatThrownBy(() -> service.create(request(badLevels)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unique rank");
    }

    @Test
    void rejectsMoreThanOneDefaultPurchaseLevel() {
        when(templateRepository.existsByNameIgnoreCase("Pharma Tablet Pack")).thenReturn(false);
        List<UomConversionTemplateLevelRequest> badLevels = List.of(
            new UomConversionTemplateLevelRequest(1L, 0, BigDecimal.ONE, false),
            new UomConversionTemplateLevelRequest(2L, 1, new BigDecimal("10"), true),
            new UomConversionTemplateLevelRequest(3L, 2, new BigDecimal("100"), true));

        assertThatThrownBy(() -> service.create(request(badLevels)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only one level");
    }

    @Test
    void updateExcludesItselfFromDuplicateNameCheck() {
        UomConversionTemplate existing = new UomConversionTemplate();
        existing.setId(5L);
        existing.setBaseUom(tablet);
        when(templateRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(templateRepository.existsByNameIgnoreCaseAndIdNot("Pharma Tablet Pack", 5L)).thenReturn(false);

        UomConversionTemplateResponse response = service.update(5L, request(validLevels()));

        assertThat(response.name()).isEqualTo("Pharma Tablet Pack");
    }

    @Test
    void throwsWhenUpdatingUnknownTemplate() {
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request(validLevels())))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteThrowsWhenTemplateNotFound() {
        when(templateRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllFiltersByBaseUomIdWhenGiven() {
        UomConversionTemplate t = new UomConversionTemplate();
        t.setId(7L);
        t.setBaseUom(tablet);
        t.setName("Pharma Tablet Pack");
        when(templateRepository.findByBaseUomIdAndIsActiveTrueOrderByNameAsc(1L)).thenReturn(List.of(t));

        List<UomConversionTemplateResponse> result = service.findAll(false, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).baseUomId()).isEqualTo(1L);
    }
}
