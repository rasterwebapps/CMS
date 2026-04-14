package com.cms.dto;

import com.cms.model.enums.ConfigDataType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SystemConfigurationRequest(
    @NotBlank(message = "Config key is required")
    String configKey,

    @NotBlank(message = "Config value is required")
    String configValue,

    String description,

    @NotNull(message = "Data type is required")
    ConfigDataType dataType,

    @NotBlank(message = "Category is required")
    String category,

    Boolean isEditable
) {}
