package com.cms.spatial.dto;

import java.time.Instant;

import com.cms.spatial.model.enums.ShapeType;
import com.cms.spatial.model.enums.VirtualLocationStatus;

public record VirtualLocationResponse(
    Long id,
    Long floorPlanId,
    String entityType,
    Long entityId,
    String name,
    String locationType,
    String moduleTag,
    ShapeType shapeType,
    String geometryJson,
    Integer capacity,
    VirtualLocationStatus status,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
