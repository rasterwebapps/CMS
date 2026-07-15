package com.cms.dto;

import jakarta.validation.constraints.NotNull;

public record LibrarySettingUpdateRequest(
    @NotNull(message = "Setting value is required")
    String settingValue
) {}
