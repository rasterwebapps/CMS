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
    ClassScheduleStatus status
) {}
