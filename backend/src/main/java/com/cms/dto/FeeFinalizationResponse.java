package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeFinalizationResponse(
    Long enquiryId,
    BigDecimal finalizedTotalFee,
    BigDecimal finalizedDiscountAmount,
    String finalizedDiscountReason,
    BigDecimal finalizedNetFee,
    String finalizedBy,
    Instant finalizedAt,
    String status
) {}
