package com.cms.inventory.consignment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ConsignmentStockLineResponse(
    Long id,
    Long agreementId,
    String agreementNumber,
    Long supplierId,
    String supplierName,
    Long locationId,
    String locationVirtualName,
    Long productId,
    String productCode,
    String productName,
    BigDecimal consignmentPrice,
    BigDecimal receivedQty,
    BigDecimal consumedQty,
    BigDecimal qtyOnHand,
    String lastReceivedBy,
    Instant lastReceivedAt,
    String lastConsumedBy,
    Instant lastConsumedAt,
    Instant createdAt,
    Instant updatedAt
) {}
