package com.cms.dto;

/** One unstaffed cell {@link com.cms.service.TimetableStaffingAutoAssignService} could not staff. */
public record AutoStaffUnplacedItem(
    String subjectName,
    String reason
) {}
