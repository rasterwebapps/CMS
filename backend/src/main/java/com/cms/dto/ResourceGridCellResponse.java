package com.cms.dto;

import java.time.LocalTime;

import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;

public record ResourceGridCellResponse(
    Long sessionId,
    String subjectName,
    String subjectCode,
    String roomName,
    String facultyName,
    String batchName,
    LocalTime startTime,
    LocalTime endTime,
    String slotName,
    ClassSessionType sessionType,
    ClassScheduleStatus status,
    /** True only for a synthetic Clinical Shift cell (bus-depart through bus-return) — it has no
     *  backing {@code ClassSchedule} row, so {@code sessionId} is a negative, non-clickable
     *  placeholder id. See {@code ResourceGridService#toShiftCell}. */
    boolean isOffCampusShift
) {}
