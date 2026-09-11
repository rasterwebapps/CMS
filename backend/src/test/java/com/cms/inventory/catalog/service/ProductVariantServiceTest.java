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

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.ProductAttributeValueRequest;
import com.cms.inventory.catalog.dto.ProductVariantRequest;
import com.cms.inventory.catalog.dto.ProductVariantResponse;
import com.cms.inventory.catalog.model.Category;
import com.cms.inventory.catalog.model.CategoryAttribute;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductVariant;
import com.cms.inventory.catalog.model.enums.AttributeDataType;
import com.cms.inventory.catalog.repository.CategoryAttributeRepository;
import com.cms.inventory.catalog.repository.ProductVariantRepository;

/**
 * Covers the "ProductVariant entity — parent/child SKU matrix" Phase 3 item — code/barcode
 * uniqueness (same shape as Product's own), typed attribute-value validation reused via {@code
 * TypedAttributeValueSupport}, and the "not every attribute is required at the variant level"
 * relaxation vs. {@code ProductServiceTest}'s coverage of the same rules on the product side.
 */
@ExtendWith(MockitoExtension.class)
class ProductVariantServiceTest {

    @Mock private ProductVariantRepository variantRepository;
    @Mock private CategoryAttributeRepository attributeRepository;
    @Mock private ProductService productService;

    private ProductVariantService service;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new ProductVariantService(variantRepository, attributeRepository, productService);

        category = new Category();
        category.setId(1L);
        category.setName("Apparel");

        product = new Product();
        product.setId(10L);
        product.setProductCode("SHIRT-001");
        product.setProductName("Cotton T-Shirt");
        product.setCategory(category);

        lenient().when(productService.findOrThrow(10L)).thenReturn(product);
        lenient().when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of());
        lenient().when(variantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            if (v.getId() == null) v.setId(100L);
            return v;
        });
    }

    private ProductVariantRequest request(String code, List<ProductAttributeValueRequest> attributeValues) {
        return new ProductVariantRequest(code, "Red / Large", null, null, new BigDecimal("199.00"), new BigDecimal("299.00"), true, attributeValues);
    }

    @Test
    void createsVariantUnderParentProduct() {
        ProductVariantResponse response = service.create(10L, request("SHIRT-001-RED-L", List.of()));

        assertThat(response.productId()).isEqualTo(10L);
        assertThat(response.productCode()).isEqualTo("SHIRT-001");
        assertThat(response.variantCode()).isEqualTo("SHIRT-001-RED-L");
        assertThat(response.trackingMode()).isEqualTo("NONE");
    }

    @Test
    void rejectsDuplicateVariantCode() {
        when(variantRepository.existsByVariantCodeIgnoreCase("SHIRT-001-RED-L")).thenReturn(true);

        assertThatThrownBy(() -> service.create(10L, request("SHIRT-001-RED-L", List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void rejectsDuplicateBarcode() {
        var req = new ProductVariantRequest("SHIRT-001-RED-L", "Red / Large", "8901030826001", null,
            null, null, true, List.of());
        when(variantRepository.existsByBarcodeIgnoreCase("8901030826001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(10L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void notEveryParentAttributeIsRequiredAtVariantLevel() {
        CategoryAttribute size = attribute(1L, "Size", AttributeDataType.ENUM, "Small, Medium, Large", true);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(size));

        // "Size" is marked required on the category, but omitted here — unlike ProductService,
        // this must NOT throw: only attributes actually being overridden at variant level need a
        // value, the rest fall back to reading the parent product's own value.
        ProductVariantResponse response = service.create(10L, request("SHIRT-001-RED-L", List.of()));

        assertThat(response.attributeValues()).isEmpty();
    }

    @Test
    void storesOverriddenAttributeValue() {
        CategoryAttribute size = attribute(1L, "Size", AttributeDataType.ENUM, "Small, Medium, Large", false);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(size));

        ProductVariantResponse response = service.create(10L,
            request("SHIRT-001-RED-L", List.of(new ProductAttributeValueRequest(1L, "Large"))));

        assertThat(response.attributeValues()).hasSize(1);
        assertThat(response.attributeValues().get(0).value()).isEqualTo("Large");
    }

    @Test
    void rejectsAttributeNotDefinedOnParentCategory() {
        assertThatThrownBy(() -> service.create(10L,
            request("SHIRT-001-RED-L", List.of(new ProductAttributeValueRequest(999L, "x")))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("999");
    }

    @Test
    void updateExcludesItselfFromDuplicateCheck() {
        ProductVariant existing = new ProductVariant();
        existing.setId(5L);
        existing.setProduct(product);
        when(variantRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(variantRepository.existsByVariantCodeIgnoreCaseAndIdNot("SHIRT-001-RED-L", 5L)).thenReturn(false);

        ProductVariantResponse response = service.update(10L, 5L, request("SHIRT-001-RED-L", List.of()));

        assertThat(response.variantCode()).isEqualTo("SHIRT-001-RED-L");
    }

    @Test
    void updateRejectsVariantBelongingToADifferentProduct() {
        Product otherProduct = new Product();
        otherProduct.setId(99L);
        otherProduct.setCategory(category);
        ProductVariant existing = new ProductVariant();
        existing.setId(5L);
        existing.setProduct(otherProduct);
        when(variantRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(10L, 5L, request("SHIRT-001-RED-L", List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong");
    }

    @Test
    void throwsWhenUpdatingUnknownVariant() {
        when(variantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(10L, 99L, request("SHIRT-001-RED-L", List.of())))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteThrowsWhenVariantNotFound() {
        when(variantRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void togglesActiveStatus() {
        ProductVariant existing = new ProductVariant();
        existing.setId(5L);
        existing.setProduct(product);
        existing.setIsActive(true);
        when(variantRepository.findById(5L)).thenReturn(Optional.of(existing));

        var response = service.updateStatus(5L, new ActiveStatusUpdateRequest(false, null));

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void findByBarcodeReturnsMatchingVariant() {
        ProductVariant v = new ProductVariant();
        v.setId(5L);
        v.setProduct(product);
        v.setVariantCode("SHIRT-001-RED-L");
        v.setVariantName("Red / Large");
        v.setBarcode("8901030826001");
        when(variantRepository.findByBarcodeIgnoreCase("8901030826001")).thenReturn(Optional.of(v));

        ProductVariantResponse response = service.findByBarcode("8901030826001");

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.barcode()).isEqualTo("8901030826001");
    }

    @Test
    void findByBarcodeThrowsWhenNotFound() {
        when(variantRepository.findByBarcodeIgnoreCase("0000000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByBarcode("0000000000000"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsInvalidTrackingMode() {
        var req = new ProductVariantRequest("SHIRT-001-RED-L", "Red / Large", null, "BOGUS",
            null, null, true, List.of());

        assertThatThrownBy(() -> service.create(10L, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tracking mode");
    }

    private CategoryAttribute attribute(long id, String name, AttributeDataType type, String enumOptions, boolean required) {
        CategoryAttribute a = new CategoryAttribute();
        a.setId(id);
        a.setCategory(category);
        a.setName(name);
        a.setDataType(type);
        a.setEnumOptions(enumOptions);
        a.setIsRequired(required);
        a.setDisplayOrder(0);
        return a;
    }
}
