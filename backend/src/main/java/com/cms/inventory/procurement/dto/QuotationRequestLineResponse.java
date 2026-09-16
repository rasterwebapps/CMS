package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QuotationRequestLineResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    String uomCode,
    Long purchaseRequisitionItemId,
    BigDecimal requestedQty,
    String status,
    Long awardedResponseLineId,
    Long awardedSupplierId,
    String awardedSupplierName,
    BigDecimal awardedUnitPrice,
    String awardedBy,
    Instant awardedAt,
    List<QuotationResponseLineResponse> responses
) {}
