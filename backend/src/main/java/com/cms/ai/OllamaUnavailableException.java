package com.cms.ai;

/** Thrown when the local Ollama instance can't be reached or returns an unusable response. */
public class OllamaUnavailableException extends RuntimeException {

    public OllamaUnavailableException(String message) {
        super(message);
    }

    public OllamaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
