package com.cms.inventory.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(

    @NotBlank(message = "Product code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String productCode,

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    String productName,

    @NotNull(message = "Category is required")
    Long categoryId,

    @NotNull(message = "Base unit of measure is required")
    Long baseUomId,

    BigDecimal reorderLevel,
    BigDecimal reorderQty,

    Boolean isAsset,
    Boolean isConsumable,
    Boolean isService,
    Boolean isLoanable,

    BigDecimal depreciationRate,
    Integer warrantyPeriodMonths,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    Boolean isActive,

    List<String> aliases,
    List<ProductAttributeValueRequest> attributeValues
) {}
