package com.cms.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Local Ollama connection settings -- externalized via env vars, same pattern as
 * {@code cms.minio}/{@code razorpay} in application.yml. Deliberately points only at a
 * same-network Ollama instance; AI Smart Search never calls a third-party AI provider, so no
 * student data ever leaves this deployment's own infrastructure.
 */
@Component
@ConfigurationProperties(prefix = "cms.ollama")
public class OllamaConfig {

    private String baseUrl;
    private String model;
    private int timeoutSeconds = 30;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
