package com.cms.dto;

import java.time.Instant;

public record CourseResponse(
    Long id,
    String name,
    String code,
    String specialization,
    String rollNumberCode,
    Boolean isActive,
    ProgramResponse program,
    Instant createdAt,
    Instant updatedAt
) {
    public CourseResponse(Long id, String name, String code, String specialization,
                          String rollNumberCode, ProgramResponse program, Instant createdAt, Instant updatedAt) {
        this(id, name, code, specialization, rollNumberCode, true, program, createdAt, updatedAt);
    }

    public CourseResponse(Long id, String name, String code, String specialization,
                          ProgramResponse program, Instant createdAt, Instant updatedAt) {
        this(id, name, code, specialization, null, true, program, createdAt, updatedAt);
    }
}
