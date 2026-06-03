package com.cms.dto;

import jakarta.validation.constraints.NotBlank;

public record LibrarySettingUpdateRequest(
    @NotBlank(message = "Setting value is required")
    String settingValue
) {}
