package com.cms.inventory.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.CategoryAttributeRequest;
import com.cms.inventory.catalog.dto.CategoryAttributeResponse;
import com.cms.inventory.catalog.model.Category;
import com.cms.inventory.catalog.model.CategoryAttribute;
import com.cms.inventory.catalog.model.enums.AttributeDataType;
import com.cms.inventory.catalog.repository.CategoryAttributeRepository;
import com.cms.inventory.catalog.repository.ProductAttributeValueRepository;

@Service
@Transactional(readOnly = true)
public class CategoryAttributeService {

    private final CategoryAttributeRepository attributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final CategoryService categoryService;

    public CategoryAttributeService(CategoryAttributeRepository attributeRepository,
                                     ProductAttributeValueRepository productAttributeValueRepository,
                                     CategoryService categoryService) {
        this.attributeRepository = attributeRepository;
        this.productAttributeValueRepository = productAttributeValueRepository;
        this.categoryService = categoryService;
    }

    public List<CategoryAttributeResponse> findByCategory(Long categoryId) {
        categoryService.findOrThrow(categoryId);
        return attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(categoryId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryAttributeResponse create(Long categoryId, CategoryAttributeRequest request) {
        Category category = categoryService.findOrThrow(categoryId);
        String name = requireTrimmed(request.name(), "Attribute name is required");
        AttributeDataType dataType = parseDataType(request.dataType());

        if (attributeRepository.existsByNameIgnoreCaseAndCategoryId(name, categoryId)) {
            throw new IllegalArgumentException("An attribute named '" + name + "' already exists on this category");
        }

        CategoryAttribute attribute = new CategoryAttribute();
        attribute.setCategory(category);
        attribute.setName(name);
        attribute.setDataType(dataType);
        attribute.setEnumOptions(dataType == AttributeDataType.ENUM ? trim(request.enumOptions()) : null);
        if (request.isRequired() != null) attribute.setIsRequired(request.isRequired());
        if (request.displayOrder() != null) attribute.setDisplayOrder(request.displayOrder());
        return toResponse(attributeRepository.save(attribute));
    }

    @Transactional
    public CategoryAttributeResponse update(Long categoryId, Long attributeId, CategoryAttributeRequest request) {
        CategoryAttribute attribute = findOrThrow(categoryId, attributeId);
        String name = requireTrimmed(request.name(), "Attribute name is required");
        AttributeDataType dataType = parseDataType(request.dataType());

        if (attributeRepository.existsByNameIgnoreCaseAndCategoryIdAndIdNot(name, categoryId, attributeId)) {
            throw new IllegalArgumentException("An attribute named '" + name + "' already exists on this category");
        }

        attribute.setName(name);
        attribute.setDataType(dataType);
        attribute.setEnumOptions(dataType == AttributeDataType.ENUM ? trim(request.enumOptions()) : null);
        if (request.isRequired() != null) attribute.setIsRequired(request.isRequired());
        if (request.displayOrder() != null) attribute.setDisplayOrder(request.displayOrder());
        return toResponse(attributeRepository.save(attribute));
    }

    @Transactional
    public void delete(Long categoryId, Long attributeId) {
        CategoryAttribute attribute = findOrThrow(categoryId, attributeId);
        if (productAttributeValueRepository.existsByAttributeId(attributeId)) {
            throw new IllegalArgumentException(
                "Cannot delete attribute '" + attribute.getName() + "' — one or more products already carry a value for it");
        }
        attributeRepository.delete(attribute);
    }

    private CategoryAttribute findOrThrow(Long categoryId, Long attributeId) {
        CategoryAttribute attribute = attributeRepository.findById(attributeId)
            .orElseThrow(() -> new ResourceNotFoundException("Category attribute not found with id: " + attributeId));
        if (!attribute.getCategory().getId().equals(categoryId)) {
            throw new ResourceNotFoundException("Category attribute not found with id: " + attributeId);
        }
        return attribute;
    }

    private AttributeDataType parseDataType(String value) {
        try {
            return AttributeDataType.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid data type '" + value + "' — must be one of TEXT, NUMBER, DATE, BOOLEAN, ENUM");
        }
    }

    private CategoryAttributeResponse toResponse(CategoryAttribute a) {
        return new CategoryAttributeResponse(a.getId(), a.getCategory().getId(), a.getName(),
            a.getDataType().name(), a.getEnumOptions(), a.getIsRequired(), a.getDisplayOrder(),
            a.getCreatedAt(), a.getUpdatedAt());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) throw new IllegalArgumentException(message);
        return t;
    }
}
