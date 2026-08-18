package com.cms.spatial.dto;

import java.time.Instant;

import com.cms.spatial.model.enums.OriginAnchor;
import com.cms.spatial.model.enums.UnitSystem;

public record FloorPlanResponse(
    Long id,
    String entityType,
    Long entityId,
    String name,
    String originalFileName,
    String originalContentType,
    UnitSystem unitSystem,
    OriginAnchor originAnchor,
    Double originX,
    Double originY,
    Double viewboxWidth,
    Double viewboxHeight,
    Double scaleFactor,
    Double calibrationPoint1X,
    Double calibrationPoint1Y,
    Double calibrationPoint2X,
    Double calibrationPoint2Y,
    Double calibrationPhysicalLength,
    Boolean isCalibrated,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
