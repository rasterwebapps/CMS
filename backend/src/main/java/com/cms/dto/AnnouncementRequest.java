package com.cms.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record AnnouncementRequest(
    @NotBlank(message = "Title is required")
    String title,

    @NotBlank(message = "Body is required")
    String body,

    @NotEmpty(message = "At least one audience target is required")
    @Valid
    List<AnnouncementAudienceRequest> audiences
) {}
