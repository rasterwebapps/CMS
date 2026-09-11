package com.cms.inventory.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
    Long id,
    String productCode,
    String productName,
    String barcode,
    Long categoryId,
    String categoryName,
    Long baseUomId,
    String baseUomCode,
    String baseUomName,
    Long brandId,
    String brandName,
    BigDecimal reorderLevel,
    BigDecimal reorderQty,
    BigDecimal standardCost,
    BigDecimal listPrice,
    String hsnSacCode,
    /** A TaxRule id, not resolved to a name here — see the field's javadoc on {@code Product}
     *  for why Catalog doesn't hold a relationship to procurement's TaxRule. The frontend
     *  resolves the display name from its own already-loaded tax-rule list. */
    Long defaultTaxRuleId,
    Boolean isAsset,
    Boolean isConsumable,
    Boolean isService,
    Boolean isLoanable,
    String trackingMode,
    BigDecimal depreciationRate,
    Integer warrantyPeriodMonths,
    BigDecimal lengthCm,
    BigDecimal widthCm,
    BigDecimal heightCm,
    BigDecimal weightKg,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    List<String> aliases,
    List<ProductAttributeValueResponse> attributeValues
) {}
