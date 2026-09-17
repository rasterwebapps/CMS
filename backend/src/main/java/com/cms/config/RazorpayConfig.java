package com.cms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Razorpay credentials -- externalized via env vars, same pattern as {@code cms.minio}/
 *  {@code keycloak.admin} in application.yml. The key secret and webhook secret are only ever
 *  used server-side (order creation, webhook signature verification); the key id alone is handed
 *  to the frontend for Razorpay's hosted Checkout.js widget. */
@Component
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayConfig {

    private String keyId;
    private String keySecret;
    private String webhookSecret;

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public String getKeySecret() { return keySecret; }
    public void setKeySecret(String keySecret) { this.keySecret = keySecret; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
}
