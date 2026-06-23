package com.cms.dto;

/** Per-payment-register outcome for an inbound OneBook callback entry. */
public record OneBookWebhookResult(
    String invoiceNumber,
    String status,
    String message
) {
    public static OneBookWebhookResult ok(String invoiceNumber) {
        return new OneBookWebhookResult(invoiceNumber, "OK", null);
    }

    public static OneBookWebhookResult notFound(String invoiceNumber, String message) {
        return new OneBookWebhookResult(invoiceNumber, "NOT_FOUND", message);
    }

    public static OneBookWebhookResult invalid(String invoiceNumber, String message) {
        return new OneBookWebhookResult(invoiceNumber, "INVALID", message);
    }

    public static OneBookWebhookResult error(String invoiceNumber, String message) {
        return new OneBookWebhookResult(invoiceNumber, "ERROR", message);
    }
}
