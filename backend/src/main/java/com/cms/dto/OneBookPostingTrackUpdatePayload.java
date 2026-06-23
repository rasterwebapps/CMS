package com.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OneBook's callback right after it accepts a payment register — delivers
 * the register id it assigned. Unknown fields (the full echoed-back
 * register) are ignored; only the fields OneCMS actually needs are bound.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OneBookPostingTrackUpdatePayload(
    String invoiceNumber,
    String documentNumber,
    String oneBookPaymentRegisterId,
    String status,
    String message,
    String comment
) {
    /** invoiceNumber and documentNumber are always the same generated number OneCMS sent — either identifies the register. */
    public String correlationKey() {
        return (invoiceNumber != null && !invoiceNumber.isBlank()) ? invoiceNumber : documentNumber;
    }
}
