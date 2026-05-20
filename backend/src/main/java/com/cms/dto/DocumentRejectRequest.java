package com.cms.dto;

import jakarta.validation.constraints.NotBlank;

public record DocumentRejectRequest(
    @NotBlank(message = "Rejection comment is required")
    String rejectionComment
) {}
