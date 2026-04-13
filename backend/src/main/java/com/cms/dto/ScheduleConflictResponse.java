package com.cms.dto;

import java.util.List;

public record ScheduleConflictResponse(
    boolean hasConflict,
    List<String> labConflicts,
    List<String> facultyConflicts,
    List<String> batchConflicts
) {}
