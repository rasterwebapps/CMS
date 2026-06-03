package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.SettingDataType;

public record LibrarySettingResponse(
    Long id,
    String settingKey,
    String settingValue,
    String displayName,
    String description,
    SettingDataType dataType,
    Instant updatedAt
) {}
