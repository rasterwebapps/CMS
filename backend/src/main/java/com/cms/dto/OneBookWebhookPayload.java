package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Incoming callback payload from OneBook's cronjob.
 * Unknown fields are silently ignored so future OneBook additions don't break the endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OneBookWebhookPayload(
    String referenceId,
    String transactionId,
    String status,
    BigDecimal amount,
    LocalDate paidDate,
    String paymentMode,
    String remarks
) {}
