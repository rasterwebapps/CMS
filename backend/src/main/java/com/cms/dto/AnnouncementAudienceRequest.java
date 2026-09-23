package com.cms.dto;

import com.cms.model.enums.AnnouncementAudienceType;

import jakarta.validation.constraints.NotNull;

public record AnnouncementAudienceRequest(
    @NotNull(message = "Audience type is required")
    AnnouncementAudienceType audienceType,

    /** Role id / cohort id / cohort_section id depending on audienceType; must be null for ALL,
     *  required for every other type -- validated in the service, not here, since the rule
     *  depends on the sibling field. */
    Long audienceRefId
) {}
