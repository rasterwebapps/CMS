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

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.CategoryAttributeRequest;
import com.cms.inventory.catalog.dto.CategoryAttributeResponse;
import com.cms.inventory.catalog.model.Category;
import com.cms.inventory.catalog.model.CategoryAttribute;
import com.cms.inventory.catalog.repository.CategoryAttributeRepository;
import com.cms.inventory.catalog.repository.ProductAttributeValueRepository;
import com.cms.inventory.catalog.repository.ProductVariantAttributeValueRepository;

/**
 * Covers CategoryAttributeService's basic CRUD plus the delete guard extended for the
 * 2026-09-11 "ProductVariant" item — a category attribute already in use by a product OR a
 * product variant must not be deletable.
 */
@ExtendWith(MockitoExtension.class)
class CategoryAttributeServiceTest {

    @Mock private CategoryAttributeRepository attributeRepository;
    @Mock private ProductAttributeValueRepository productAttributeValueRepository;
    @Mock private ProductVariantAttributeValueRepository variantAttributeValueRepository;
    @Mock private CategoryService categoryService;

    private CategoryAttributeService service;

    private Category category;

    @BeforeEach
    void setUp() {
        service = new CategoryAttributeService(attributeRepository, productAttributeValueRepository,
            variantAttributeValueRepository, categoryService);

        category = new Category();
        category.setId(1L);
        category.setName("Apparel");

        lenient().when(categoryService.findOrThrow(1L)).thenReturn(category);
        lenient().when(attributeRepository.save(any(CategoryAttribute.class))).thenAnswer(inv -> {
            CategoryAttribute a = inv.getArgument(0);
            if (a.getId() == null) a.setId(100L);
            return a;
        });
    }

    private CategoryAttributeRequest request() {
        return new CategoryAttributeRequest("Size", "ENUM", "Small, Medium, Large", true, 1);
    }

    @Test
    void createsAttribute() {
        when(attributeRepository.existsByNameIgnoreCaseAndCategoryId("Size", 1L)).thenReturn(false);

        CategoryAttributeResponse response = service.create(1L, request());

        assertThat(response.name()).isEqualTo("Size");
        assertThat(response.dataType()).isEqualTo("ENUM");
    }

    @Test
    void rejectsDuplicateNameOnCreate() {
        when(attributeRepository.existsByNameIgnoreCaseAndCategoryId("Size", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, request()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void rejectsInvalidDataType() {
        var req = new CategoryAttributeRequest("Size", "BOGUS", null, false, 1);

        assertThatThrownBy(() -> service.create(1L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid data type");
    }

    @Test
    void deleteThrowsWhenAttributeInUseByAProduct() {
        CategoryAttribute attribute = attribute(5L);
        when(attributeRepository.findById(5L)).thenReturn(Optional.of(attribute));
        when(productAttributeValueRepository.existsByAttributeId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L, 5L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("products already carry a value");
    }

    @Test
    void deleteThrowsWhenAttributeInUseByAVariant() {
        CategoryAttribute attribute = attribute(5L);
        when(attributeRepository.findById(5L)).thenReturn(Optional.of(attribute));
        when(productAttributeValueRepository.existsByAttributeId(5L)).thenReturn(false);
        when(variantAttributeValueRepository.existsByAttributeId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L, 5L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("variants already carry a value");
    }

    @Test
    void deleteSucceedsWhenAttributeUnused() {
        CategoryAttribute attribute = attribute(5L);
        when(attributeRepository.findById(5L)).thenReturn(Optional.of(attribute));
        when(productAttributeValueRepository.existsByAttributeId(5L)).thenReturn(false);
        when(variantAttributeValueRepository.existsByAttributeId(5L)).thenReturn(false);

        service.delete(1L, 5L);
    }

    @Test
    void deleteThrowsWhenAttributeNotFound() {
        when(attributeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteThrowsWhenAttributeBelongsToADifferentCategory() {
        Category otherCategory = new Category();
        otherCategory.setId(2L);
        CategoryAttribute attribute = new CategoryAttribute();
        attribute.setId(5L);
        attribute.setCategory(otherCategory);
        when(attributeRepository.findById(5L)).thenReturn(Optional.of(attribute));

        assertThatThrownBy(() -> service.delete(1L, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private CategoryAttribute attribute(long id) {
        CategoryAttribute a = new CategoryAttribute();
        a.setId(id);
        a.setCategory(category);
        a.setName("Size");
        return a;
    }
}
