package com.cms.dto;

import java.time.Instant;

public record NotificationResponse(
    Long id,
    String categoryKey,
    String title,
    String message,
    String link,
    Instant createdAt
) {}
