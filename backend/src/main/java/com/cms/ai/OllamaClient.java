package com.cms.ai;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin HTTP client for a locally-hosted Ollama instance's {@code /api/chat} endpoint, used in
 * JSON mode so the model's reply is always a parseable JSON string. This is the only network
 * call AI Smart Search makes -- there is no third-party AI provider involved anywhere in this
 * feature.
 */
@Component
public class OllamaClient {

    private final RestClient restClient;
    private final OllamaConfig config;

    public OllamaClient(OllamaConfig config) {
        this.config = config;
        this.restClient = RestClient.builder()
            .baseUrl(config.getBaseUrl())
            .requestFactory(clientHttpRequestFactory(config.getTimeoutSeconds()))
            .build();
    }

    private static org.springframework.http.client.ClientHttpRequestFactory clientHttpRequestFactory(int timeoutSeconds) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        int timeoutMs = (int) Duration.ofSeconds(timeoutSeconds).toMillis();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return factory;
    }

    /**
     * Sends a system + user prompt pair and returns the model's raw JSON reply as a string.
     * Callers (e.g. {@link StudentSearchIntentParser}) are responsible for parsing and
     * validating that string -- this method makes no assumption about its shape.
     */
    public String chatJson(String systemPrompt, String userPrompt) {
        ChatRequest request = new ChatRequest(
            config.getModel(),
            List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt)
            ),
            "json",
            false
        );

        ChatResponse response = restClient.post()
            .uri("/api/chat")
            .body(request)
            .retrieve()
            .body(ChatResponse.class);

        if (response == null || response.message() == null) {
            throw new OllamaUnavailableException("Local Ollama instance returned an empty response");
        }
        return response.message().content();
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatRequest(String model, List<ChatMessage> messages, String format, boolean stream) {
    }

    private record ChatResponse(ChatMessage message) {
    }
}
