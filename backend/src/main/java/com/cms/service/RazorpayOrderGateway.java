package com.cms.service;

/** Thin seam around the Razorpay SDK's order-creation call -- exists purely so
 *  {@link RazorpayPaymentService} can be unit-tested without a real network call or fighting the
 *  SDK's public-field client design ({@code RazorpayClient.orders} is a concrete field set only
 *  inside its package-private constructor, not mockable via a mocked {@code RazorpayClient}). */
public interface RazorpayOrderGateway {

    /** Creates a Razorpay order and returns its {@code id}. Throws
     *  {@link org.springframework.web.server.ResponseStatusException} (502) on any gateway
     *  failure -- never a checked {@code RazorpayException} leaking into callers. */
    String createOrder(long amountPaise, String currency, String receipt);
}
