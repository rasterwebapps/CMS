package com.cms.dto;

import java.math.BigDecimal;

/** Handed to the frontend to open Razorpay's hosted Checkout.js widget -- {@code razorpayKeyId}
 *  is the public key only, never the key secret. */
public record RazorpayOrderResponse(
    String razorpayOrderId,
    BigDecimal amount,
    String currency,
    String razorpayKeyId
) {
}
