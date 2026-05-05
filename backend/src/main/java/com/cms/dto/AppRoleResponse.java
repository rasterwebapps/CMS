package com.cms.dto;

import java.util.List;

public record AppRoleResponse(
    Long id,
    String name,
    String displayName,
    int hierarchyLevel,
    boolean isSystemRole,
    String description,
    List<String> permissionCodes
) {}
