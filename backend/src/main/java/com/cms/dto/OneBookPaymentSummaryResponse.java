package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OneBookPaymentSummaryResponse(
    String  referenceId,
    String  invoiceNumber,
    String  status,
    String  onebookStatus,
    BigDecimal amount,
    Instant transmittedAt,
    String  errorMessage,
    String  onebookRemarks,
    Instant createdAt
) {}
