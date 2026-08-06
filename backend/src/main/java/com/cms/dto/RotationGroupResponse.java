package com.cms.dto;

import java.time.LocalDate;
import java.util.List;

import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

public record RotationGroupResponse(
    Long id,
    Long termInstanceId,
    String label,
    Integer cycleLength,
    LocalDate anchorOccurrenceDate,
    List<RotationSlotResponse> slots,
    List<RotationMemberResponse> members,
    List<String> warnings
) {

    public record RotationSlotResponse(
        Long id,
        Long classScheduleId,
        Integer slotOrder,
        String subjectName,
        ClassSessionType sessionType,
        DayOfWeek dayOfWeek,
        String periodName
    ) {}

    public record RotationMemberResponse(
        Long id,
        Integer memberOrder,
        String label,
        List<RotationAssignmentResponse> assignments
    ) {}

    public record RotationAssignmentResponse(
        Long rotationSlotId,
        Long classScheduleId,
        Long batchId,
        String batchName
    ) {}
}
