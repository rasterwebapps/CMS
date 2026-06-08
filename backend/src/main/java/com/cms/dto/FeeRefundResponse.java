package com.cms.dto;

import java.math.BigDecimal;

/** Returned when a refund request is initiated (status = PENDING). */
public record FeeRefundResponse(
    Long id,
    String originalReceiptNumber,
    BigDecimal refundAmount,
    String reason,
    String studentName,
    String rollNumber,
    String status
) {}
