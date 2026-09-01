package com.cms.dto;

import java.time.LocalDate;

public record EscortDutyDto(
    LocalDate date,
    Long batchId,
    String batchName,
    Long facultyId,
    String facultyName,
    String clinicalVenueName
) {}
