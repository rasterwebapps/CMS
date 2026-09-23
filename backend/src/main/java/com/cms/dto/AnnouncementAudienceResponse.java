package com.cms.dto;

import com.cms.model.enums.AnnouncementAudienceType;

public record AnnouncementAudienceResponse(
    AnnouncementAudienceType audienceType,
    Long audienceRefId,
    String audienceLabel
) {}
