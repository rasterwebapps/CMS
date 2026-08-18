package com.cms.spatial.dto;

import com.cms.spatial.model.enums.OriginAnchor;
import com.cms.spatial.model.enums.UnitSystem;

/**
 * Service-facing creation payload. Floor plans are created together with their initial file
 * (storage_key is NOT NULL in the schema — there is no metadata-only create), so the controller
 * assembles this from the multipart form fields before calling the service.
 */
public record FloorPlanCreateRequest(
    String entityType,
    Long entityId,
    String name,
    UnitSystem unitSystem,
    OriginAnchor originAnchor,
    Double originX,
    Double originY,
    Double viewboxWidth,
    Double viewboxHeight
) {}
