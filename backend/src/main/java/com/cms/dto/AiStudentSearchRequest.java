package com.cms.dto;

/** Request body for AI Smart Search's student endpoint -- a single free-text query. */
public record AiStudentSearchRequest(String query) {
}
