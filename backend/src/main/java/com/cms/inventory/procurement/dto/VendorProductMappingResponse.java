package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record VendorProductMappingResponse(
    Long id,
    Long supplierId,
    String supplierName,
    Long productId,
    String productCode,
    String productName,
    Long rateContractId,
    String vendorPartNumber,
    String vendorProductName,
    BigDecimal unitPrice,
    String currencyCode,
    Long uomId,
    String uomCode,
    BigDecimal minOrderQty,
    Integer leadTimeDays,
    Boolean isPreferred,
    Boolean isActive,

    /** unitPrice, unless an active, in-window RateContract line for this product overrides it. */
    BigDecimal effectivePrice,
    /** "CONTRACT" when effectivePrice came from a RateContractLine, "STANDARD" otherwise. */
    String priceSource,

    /** The institution's configured base currency, or null if not configured yet. */
    String baseCurrencyCode,
    /** effectivePrice converted to baseCurrencyCode, or null when no base currency is configured
     *  or no exchange rate is on file for this mapping's currency — never a computation error,
     *  just "not resolvable right now". See CurrencyExchangeRateService.resolveToBaseCurrency. */
    BigDecimal effectivePriceInBaseCurrency,

    Instant createdAt,
    Instant updatedAt
) {}
