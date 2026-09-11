package com.cms.inventory.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.inventory.catalog.dto.ProductAttributeValueRequest;
import com.cms.inventory.catalog.dto.ProductRequest;
import com.cms.inventory.catalog.dto.ProductResponse;
import com.cms.inventory.catalog.model.Category;
import com.cms.inventory.catalog.model.CategoryAttribute;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.model.enums.AttributeDataType;
import com.cms.inventory.catalog.repository.CategoryAttributeRepository;
import com.cms.inventory.catalog.repository.ProductRepository;

/**
 * Covers the typed EAV storage behavior {@code ProductService.applyAttributeValues}/
 * {@code applyTypedValue} added for the "Typed EAV storage" Phase 1 item — each
 * {@code CategoryAttribute.dataType} parses and validates its submitted string differently, and a
 * round trip through the typed column must render back to the same plain-string form.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryAttributeRepository attributeRepository;
    @Mock private CategoryService categoryService;
    @Mock private UomService uomService;
    @Mock private BrandService brandService;

    private ProductService service;

    private Category category;
    private Uom uom;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, attributeRepository, categoryService, uomService, brandService);

        category = new Category();
        category.setId(1L);
        category.setName("Chemicals");

        uom = new Uom();
        uom.setId(1L);
        uom.setCode("KG");
        uom.setName("Kilogram");

        when(categoryService.findOrThrow(1L)).thenReturn(category);
        when(uomService.findOrThrow(1L)).thenReturn(uom);
        lenient().when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
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

    private ProductRequest request(List<ProductAttributeValueRequest> attributeValues) {
        return new ProductRequest("CHM-0001", "Sodium Chloride", 1L, 1L, null,
            null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, true,
            List.of(), attributeValues);
    }

    @Test
    void storesAndRendersNumberAttribute() {
        CategoryAttribute shelfLifeMonths = attribute(10L, "Shelf Life (months)", AttributeDataType.NUMBER, null, false);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(shelfLifeMonths));

        ProductResponse response = service.create(request(List.of(new ProductAttributeValueRequest(10L, "24"))));

        assertThat(response.attributeValues()).hasSize(1);
        assertThat(response.attributeValues().get(0).value()).isEqualTo("24");
    }

    @Test
    void rejectsNonNumericValueForNumberAttribute() {
        CategoryAttribute shelfLifeMonths = attribute(10L, "Shelf Life (months)", AttributeDataType.NUMBER, null, false);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(shelfLifeMonths));

        assertThatThrownBy(() -> service.create(request(List.of(new ProductAttributeValueRequest(10L, "twenty-four")))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Shelf Life (months)")
            .hasMessageContaining("number");
    }

    @Test
    void storesAndRendersDateAttribute() {
        CategoryAttribute calibrationDue = attribute(11L, "Calibration Due", AttributeDataType.DATE, null, false);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(calibrationDue));

        ProductResponse response = service.create(request(List.of(new ProductAttributeValueRequest(11L, "2026-12-31"))));

        assertThat(response.attributeValues().get(0).value()).isEqualTo("2026-12-31");
    }

    @Test
    void rejectsUnparsableDateAttribute() {
        CategoryAttribute calibrationDue = attribute(11L, "Calibration Due", AttributeDataType.DATE, null, false);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(calibrationDue));

        assertThatThrownBy(() -> service.create(request(List.of(new ProductAttributeValueRequest(11L, "31/12/2026")))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("date");
    }

    @Test
    void storesAndRendersBooleanAttribute() {
        CategoryAttribute isFragile = attribute(12L, "Fragile", AttributeDataType.BOOLEAN, null, false);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(isFragile));

        ProductResponse response = service.create(request(List.of(new ProductAttributeValueRequest(12L, "true"))));

        assertThat(response.attributeValues().get(0).value()).isEqualTo("true");
    }

    @Test
    void rejectsNonBooleanValueForBooleanAttribute() {
        CategoryAttribute isFragile = attribute(12L, "Fragile", AttributeDataType.BOOLEAN, null, false);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(isFragile));

        assertThatThrownBy(() -> service.create(request(List.of(new ProductAttributeValueRequest(12L, "yes")))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("true or false");
    }

    @Test
    void storesAndRendersEnumAttributeWithinAllowedOptions() {
        CategoryAttribute storageCondition = attribute(13L, "Storage Condition", AttributeDataType.ENUM,
            "Room Temperature, Refrigerated, Frozen", false);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(storageCondition));

        ProductResponse response = service.create(request(List.of(new ProductAttributeValueRequest(13L, "Refrigerated"))));

        assertThat(response.attributeValues().get(0).value()).isEqualTo("Refrigerated");
    }

    @Test
    void rejectsEnumValueOutsideAllowedOptions() {
        CategoryAttribute storageCondition = attribute(13L, "Storage Condition", AttributeDataType.ENUM,
            "Room Temperature, Refrigerated, Frozen", false);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(storageCondition));

        assertThatThrownBy(() -> service.create(request(List.of(new ProductAttributeValueRequest(13L, "Boiling")))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Storage Condition")
            .hasMessageContaining("Room Temperature");
    }

    @Test
    void storesTextAttributeAsIs() {
        CategoryAttribute warrantyProvider = attribute(14L, "Warranty Provider", AttributeDataType.TEXT, null, false);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(warrantyProvider));

        ProductResponse response = service.create(request(List.of(new ProductAttributeValueRequest(14L, "Dell ProSupport"))));

        assertThat(response.attributeValues().get(0).value()).isEqualTo("Dell ProSupport");
    }

    @Test
    void requiredAttributeMustBeSubmitted() {
        CategoryAttribute storageCondition = attribute(13L, "Storage Condition", AttributeDataType.ENUM,
            "Room Temperature, Refrigerated, Frozen", true);
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of(storageCondition));

        assertThatThrownBy(() -> service.create(request(List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Storage Condition")
            .hasMessageContaining("required");
    }

    @Test
    void rejectsAttributeIdNotDefinedOnCategory() {
        when(attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(request(List.of(new ProductAttributeValueRequest(999L, "x")))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("999");
    }
}
