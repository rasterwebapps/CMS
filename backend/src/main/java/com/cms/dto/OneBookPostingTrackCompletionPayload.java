package com.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OneBook's callback once the payment register's payment has actually been
 * completed (or failed) — carries the final payment details. Unknown fields
 * (the full echoed-back register) are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OneBookPostingTrackCompletionPayload(
    String invoiceNumber,
    String documentNumber,
    String status,
    String message,
    String comment,
    String paymentNumber,
    String bankName,
    String paymentMode,
    String transactionNumber,
    String paymentDate,
    String paymentBy,
    String batchNumber
) {
    public String correlationKey() {
        return (invoiceNumber != null && !invoiceNumber.isBlank()) ? invoiceNumber : documentNumber;
    }
}
