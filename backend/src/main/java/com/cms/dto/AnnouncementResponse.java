package com.cms.dto;

import java.time.Instant;
import java.util.List;

public record AnnouncementResponse(
    Long id,
    String title,
    String body,
    String createdBy,
    Instant publishedAt,
    List<AnnouncementAudienceResponse> audiences,
    boolean read
) {}
