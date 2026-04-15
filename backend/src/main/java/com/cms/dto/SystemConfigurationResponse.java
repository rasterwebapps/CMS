package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.ConfigDataType;

public record SystemConfigurationResponse(
    Long id,
    String configKey,
    String configValue,
    String description,
    ConfigDataType dataType,
    String category,
    Boolean isEditable,
    Instant createdAt,
    Instant updatedAt
) {}
