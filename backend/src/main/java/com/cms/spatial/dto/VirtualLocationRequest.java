package com.cms.spatial.dto;

import com.cms.spatial.model.enums.ShapeType;
import com.cms.spatial.model.enums.VirtualLocationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VirtualLocationRequest(
    @NotNull(message = "floorPlanId is required")
    Long floorPlanId,

    String entityType,

    Long entityId,

    @NotBlank(message = "name is required")
    String name,

    @NotBlank(message = "locationType is required")
    String locationType,

    String moduleTag,

    @NotNull(message = "shapeType is required")
    ShapeType shapeType,

    @NotBlank(message = "geometryJson is required")
    String geometryJson,

    Integer capacity,

    VirtualLocationStatus status,

    String description
) {}
