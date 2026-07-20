package com.cms.dto;

import java.util.List;

public record ScheduleConflictResponse(
    boolean hasConflict,
    List<String> roomConflicts,
    List<String> facultyConflicts,
    List<String> audienceConflicts
) {}
