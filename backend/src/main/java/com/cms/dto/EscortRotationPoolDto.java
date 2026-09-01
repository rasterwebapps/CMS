package com.cms.dto;

import java.time.LocalDate;
import java.util.List;

public record EscortRotationPoolDto(
    Long batchId,
    String batchName,
    Long rotationGroupId,
    Integer cycleLength,
    LocalDate anchorOccurrenceDate,
    List<EscortRotationMemberDto> members
) {
    public record EscortRotationMemberDto(
        Integer memberOrder,
        Long facultyId,
        String facultyName
    ) {}
}
