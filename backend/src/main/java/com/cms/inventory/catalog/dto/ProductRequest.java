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

    /** The product's real-world barcode/GTIN, as captured from packaging — optional, unique when
     *  present. */
    @Size(max = 64, message = "Barcode must not exceed 64 characters")
    String barcode,

    @NotNull(message = "Category is required")
    Long categoryId,

    @NotNull(message = "Base unit of measure is required")
    Long baseUomId,

    /** Optional — a generic lab consumable commonly has none. */
    Long brandId,

    BigDecimal reorderLevel,
    BigDecimal reorderQty,

    /** Baseline cost, independent of any one supplier's negotiated VendorProductMapping rate. */
    BigDecimal standardCost,
    /** List price / MRP — the default reference selling price. */
    BigDecimal listPrice,

    @Size(max = 20, message = "HSN/SAC code must not exceed 20 characters")
    String hsnSacCode,

    /** A Purchase Order line pre-fills its tax from this TaxRule id when none is explicitly
     *  chosen — an override always stays available on the line itself. Not validated for
     *  existence here (see the field's javadoc on {@code Product} for the module-boundary
     *  reason); Procurement resolves/validates it when it actually uses it. */
    Long defaultTaxRuleId,

    Boolean isAsset,
    Boolean isConsumable,
    Boolean isService,
    Boolean isLoanable,

    /** NONE, BATCH, or SERIAL — null/blank defaults to NONE. */
    String trackingMode,

    BigDecimal depreciationRate,
    Integer warrantyPeriodMonths,

    BigDecimal lengthCm,
    BigDecimal widthCm,
    BigDecimal heightCm,
    BigDecimal weightKg,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    Boolean isActive,

    List<String> aliases,
    List<ProductAttributeValueRequest> attributeValues
) {}
