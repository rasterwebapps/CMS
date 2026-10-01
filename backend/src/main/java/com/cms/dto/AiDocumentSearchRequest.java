package com.cms.dto;

/** Request body for the Admission document RAG search endpoint (OC-277) -- a free-text question. */
public record AiDocumentSearchRequest(String query) {
}
