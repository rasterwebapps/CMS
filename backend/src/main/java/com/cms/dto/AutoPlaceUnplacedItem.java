package com.cms.dto;

import com.cms.model.enums.ClassSessionType;

/** One shortfall unit {@link com.cms.service.TimetableSkeletonAutoPlaceService} could not place. */
public record AutoPlaceUnplacedItem(
    String subjectName,
    ClassSessionType sessionType,
    String occupantLabel,
    String reason
) {}
